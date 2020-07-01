package au.org.ala.dataqualityfilter

import grails.converters.JSON
import grails.plugin.cache.CacheEvict
import grails.plugin.cache.Cacheable
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FirstParam
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.HeadMethod
import org.apache.commons.httpclient.util.URIUtil
import org.apache.commons.io.FileUtils
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONElement
import org.grails.web.json.JSONObject
import org.springframework.web.client.RestClientException
import org.supercsv.cellprocessor.ift.CellProcessor
import org.supercsv.io.CsvListReader
import org.supercsv.io.ICsvListReader
import org.supercsv.prefs.CsvPreference

import javax.annotation.PostConstruct

import static org.apache.commons.lang3.StringUtils.replace

/**
 * Service to perform web service DAO operations
 */
class WebServicesService {

    public static final String ENVIRONMENTAL = "Environmental"
    public static final String CONTEXTUAL = "Contextual"
    def grailsApplication, facetsCacheServiceBean
    QualityService qualityService

    Map cachedGroupedFacets = [:] // keep a copy in case method throws an exception and then blats the saved version

    @PostConstruct
    def init(){
        facetsCacheServiceBean = grailsApplication.mainContext.getBean('facetsCacheService')
    }

    JSONObject fullTextSearch(SpatialSearchRequestParams requestParams) {
        def result = applyQualityFiltersToJsonRequest(requestParams) { newParams ->
            def url = "${grailsApplication.config.biocache.baseUrl}/occurrences/search?${newParams.getEncodedParams()}"
            getJsonElements(url)
        }
        return result
    }

    private JSONObject applyQualityFiltersToJsonRequest(SearchRequestParams requestParams, @ClosureParams(FirstParam) Closure<JSONObject> f) {
        SearchRequestParams newParams = convertSearchRequestParamsForDataQualitySettings(requestParams)
        JSONObject result = f(newParams)
        return convertSearchResultsForDataQualitySettings(newParams, result)
    }

    private def convertSearchRequestParamsForDataQualitySettings(SearchRequestParams requestParams) {
        def newParams = requestParams.clone()
        // Transmute the disableQualityFilter params into data quality filter querys (dqfqs)
        def dqqfs = newParams.disableQualityFilter
        def skip = newParams.disableAllQualityFilters
        List<String> appliedFilters = []
        if (!skip) {
            def disabled = dqqfs as Set
            appliedFilters = qualityService.getEnabledFiltersByLabel(requestParams.qualityProfile)
                    .findAll { label, filters -> !disabled.contains(label) }
                    .collect { label, filters -> filters }
        }
        newParams.dqfq = appliedFilters
        return newParams
    }

    private JSONObject convertSearchResultsForDataQualitySettings(SearchRequestParams newParams, JSONObject result) {
        // Fix the results to remove the dqfqs from queryString and urlParams and active facets
        def activeFacetMapFilterLookup = result?.activeFacetMap?.collectEntries { k, v -> [(String.valueOf(k) + ':' + String.valueOf(v?.value)): k]} ?: [:]
        def activeFacetObjFilterLookup = result?.activeFacetObj?.collectEntries { String k, List v ->
            v.withIndex().collectEntries { element, index -> [(element.value ?: '') : [ key: k, idx: index]] }
        } ?: [:]
        log.debug('{}', activeFacetMapFilterLookup)
        newParams.dqfq?.each { filter ->
            def encoded = URIUtil.encodeWithinQuery(filter) //simpleEncode(filter)

            if (result.query) {
                result.query = fixFq(result.query, encoded)
            }
            if (result.urlParameters) {
                result.urlParameters = fixFq(result.urlParameters, encoded)
            }

            def activeFacetMapKey = activeFacetMapFilterLookup[filter]
            def activeFacetObjKey = activeFacetObjFilterLookup[filter]

            if (activeFacetMapKey) {
                result?.activeFacetMap?.remove(activeFacetMapKey)
            }
            if (activeFacetObjKey) {
                def key = activeFacetObjKey.key
                def idx = activeFacetObjKey.idx
                def activeFacetList = result.activeFacetObj[key]
                activeFacetList[idx] = null
            }
        }
        result.activeFacetObj?.each { entry -> entry.value = entry.value.findAll { it != null } }
        String extraParams = newParams.disableQualityFilter.collect { "disableQualityFilter=$it" }.join('&')
        if (result.urlParameters) {
            result.urlParameters += extraParams ? '&' + extraParams : ''
            if (newParams.disableAllQualityFilters) {
                result.urlParameters += '&disableAllQualityFilters=true'
            }
            if (newParams.qualityProfile) {
                result.urlParameters += "&qualityProfile=${newParams.qualityProfile}"
            }
        }
        return result
    }

    def fixFq(String queryParams, String fixFq, String replace = null) {
        def startsWithQ = queryParams.startsWith('?')
        if (startsWithQ) {
            queryParams = queryParams.substring(1)
        }
        def tokens = queryParams.tokenize('&')
        if (replace != null) {
            tokens = tokens.collect { it == 'fq=' + fixFq ? replace : it }
        } else {
            tokens.remove('fq='+ fixFq)
        }
        return (startsWithQ ? '?' : '') + tokens.join('&')
    }

    /**
     * Try to replicate the URL encoding scheme the service sends back.
     * @param str The string to encode
     * @return The encoded string
     */
    def simpleEncode(String str) {
        replace(replace(replace(replace(replace(str, ':', '%3A'), ' ', '%20'), '"', '%22'), '[', '%5B'), ']', '%5D')
    }

    JSONObject cachedFullTextSearch(SpatialSearchRequestParams requestParams) {
        fullTextSearch(requestParams)
    }

    def JSONObject getRecord(String id, Boolean hasClubView) {
        def url = "${grailsApplication.config.biocache.baseUrl}/occurrence/${id.encodeAsURL()}"
        if (hasClubView) {
            url += "?apiKey=${grailsApplication.config.biocache.apiKey?:''}"
        }
        getJsonElements(url)
    }

    def JSONObject getCompareRecord(String id) {
        def url = "${grailsApplication.config.biocache.baseUrl}/occurrence/compare?uuid=${id.encodeAsURL()}"
        getJsonElements(url)
    }

    def JSONArray getMapLegend(String queryString) {
        def url = "${grailsApplication.config.biocache.baseUrl}/mapping/legend?${queryString}"
        JSONArray json = getJsonElements(url)
        def facetName
        Map facetLabelsMap = [:]

        json.each { item ->
            if (!facetName) {
                // do this once
                facetName = item.fq?.tokenize(':')?.get(0)?.replaceFirst(/^\-/,'')
                try {
                    facetLabelsMap = facetsCacheServiceBean.getFacetNamesFor(facetName) // cached
                } catch (IllegalArgumentException iae) {
                    log.info "${iae.message}"
                }
            }

            if (facetLabelsMap && facetLabelsMap.containsKey(item.name)) {
                item.name = facetLabelsMap.get(item.name)
            }
        }
        json
    }

    def JSONArray getUserAssertions(String id) {
        def url = "${grailsApplication.config.biocache.baseUrl}/occurrences/${id.encodeAsURL()}/assertions"
        getJsonElements(url)
    }

    def JSONArray getQueryAssertions(String id) {
        def url = "${grailsApplication.config.biocache.baseUrl}/occurrences/${id.encodeAsURL()}/assertionQueries"
        getJsonElements(url)
    }

    def JSONObject getDuplicateRecordDetails(JSONObject record) {
        log.debug "getDuplicateRecordDetails -> ${record?.processed?.occurrence?.associatedOccurrences}"
        if (record?.processed?.occurrence?.associatedOccurrences) {
            def status = record.processed.occurrence.duplicationStatus
            def uuid

            if (status == "R") {
                // reference record so use its UUID
                uuid = record.raw.uuid
            } else {
                // duplicate record so use the reference record UUID
                uuid = record.processed.occurrence.associatedOccurrences
            }

            def url = "${grailsApplication.config.biocache.baseUrl}/duplicates/${uuid.encodeAsURL()}"
            getJsonElements(url)
        }
    }

    @Cacheable('longTermCache')
    def JSONArray getDefaultFacets() {
        def url = "${grailsApplication.config.biocache.baseUrl}/search/facets"
        getJsonElements(url)
    }

    @Cacheable('longTermCache')
    def JSONArray getErrorCodes() {
        def url = "${grailsApplication.config.biocache.baseUrl}/assertions/user/codes"
        getJsonElements(url)
    }

    @Cacheable(value="longTermCache", key = {"#root.method.name"})
    def Map getGroupedFacets() {
        log.info "Getting grouped facets with key: #root.methodName"
        def url = "${grailsApplication.config.biocache.baseUrl}/search/grouped/facets"

        if (grailsApplication.config.biocache.groupedFacetsUrl) {
            // some hubs use a custom JSON url
            url = "${grailsApplication.config.biocache.groupedFacetsUrl}"
        }

        Map groupedMap = [ "Custom" : []] // LinkedHashMap by default so ordering is maintained

        try {
            JSONArray groupedArray = getJsonElements(url)

            // simplify DS into a Map with key as group name and value as list of facets
            groupedArray.each { group ->
                groupedMap.put(group.title, group.facets.collect { it.field })
            }

            cachedGroupedFacets = deepCopy(groupedMap) // keep a deep copy

        } catch (Exception e) {
            log.warn "grouped facets failed to load: $e", e
            groupedMap = cachedGroupedFacets // fallback to saved copy
        }

        groupedMap
    }

    @CacheEvict(value='collectoryCache', allEntries=true)
    def doClearCollectoryCache() {
        "collectoryCache cache cleared\n"
    }

    @CacheEvict(value='longTermCache', allEntries=true)
    def doClearLongTermCache() {
        "longTermCache cache cleared\n"
    }

    /**
     * Perform POST for new assertion to biocache-service
     *
     * @param recordUuid
     * @param code
     * @param comment
     * @param userId
     * @param userDisplayName
     * @return Map postResponse
     */
    Map addAssertion(String recordUuid, String code, String comment, String userId, String userDisplayName,
                         String userAssertionStatus, String assertionUuid) {
        Map postBody =  [
                recordUuid: recordUuid,
                code: code,
                comment: comment,
                userAssertionStatus: userAssertionStatus,
                assertionUuid: assertionUuid,
                userId: userId,
                userDisplayName: userDisplayName,
                apiKey: grailsApplication.config.biocache.apiKey
        ]

        postFormData(grailsApplication.config.biocache.baseUrl + "/occurrences/assertions/add", postBody)
    }

    /**
     * Perform POST to delete an assertion on biocache-service
     *
     * @param recordUuid
     * @param assertionUuid
     * @return
     */
    def Map deleteAssertion(String recordUuid, String assertionUuid) {
        Map postBody =  [
                recordUuid: recordUuid,
                assertionUuid: assertionUuid,
                apiKey: grailsApplication.config.biocache.apiKey
        ]

        postFormData(grailsApplication.config.biocache.baseUrl + "/occurrences/assertions/delete", postBody)
    }

    @Cacheable('collectoryCache')
    def JSONObject getCollectionInfo(String id) {
        def url = "${grailsApplication.config.collections.baseUrl}/lookup/summary/${id.encodeAsURL()}"
        getJsonElements(url)
    }

    @Cacheable('collectoryCache')
    def JSONArray getCollectionContact(String id){
        def url = "${grailsApplication.config.collections.baseUrl}/ws/collection/${id.encodeAsURL()}/contact.json"
        getJsonElements(url)
    }

    @Cacheable('collectoryCache')
    def JSONArray getDataresourceContact(String id){
        def url = "${grailsApplication.config.collections.baseUrl}/ws/dataResource/${id.encodeAsURL()}/contact.json"
        getJsonElements(url)
    }

    @Cacheable('longTermCache')
    Map getLayersMetaData() {
        Map layersMetaMap = [:]
        def url = "${grailsApplication.config.layersservice.baseUrl}/layers"

        try {
            def jsonArray = getJsonElements(url)
            jsonArray.each {
                Map subset = [:]
                subset << it // clone the original Map
                subset.layerID = it.uid
                subset.layerName = it.name
                subset.layerDisplayName = it.displayname
                subset.value = null
                subset.classification1 = it.classification1
                subset.source = it.source
                subset.units = it.environmentalvalueunits

                if (it.type == ENVIRONMENTAL) {
                    layersMetaMap.put("el" + it.id, subset)
                } else if (it.type == CONTEXTUAL) {
                    layersMetaMap.put("cl" + it.id, subset)
                }
            }
        } catch (RestClientException rce) {
            log.debug "Can't access layer service - ${rce.message}"
        }

        return layersMetaMap
    }

    /**
     * Query the BIE for GUIDs for a given list of names
     *
     * @param taxaQueries
     * @return
     */
    @Cacheable('longTermCache')
    def List<String> getGuidsForTaxa(List taxaQueries) {
        List guids = []

        if (taxaQueries.size() == 1) {
            String taxaQ = taxaQueries[0]?:'*:*' // empty taxa search returns all records
            taxaQueries.addAll(taxaQ.split(" OR ") as List)
            taxaQueries.remove(0) // remove first entry
        }

        List encodedQueries = taxaQueries.collect { it.encodeAsURL() } // URL encode params

        def url = grailsApplication.config.bieService.baseUrl + "/guid/batch?q=" + encodedQueries.join("&q=")
        JSONObject guidsJson = getJsonElements(url)

        taxaQueries.each { key ->
            def match = guidsJson.get(key)[0]
            def guid = (match?.acceptedIdentifier) ? match?.acceptedIdentifier : match?.identifier
            guids.add(guid)
        }

        return guids
    }

    /**
     * Get the CSV for ALA data quality checks meta data
     *
     * @return
     */
    @Cacheable('longTermCache')
    def String getDataQualityCsv() {
        String url = grailsApplication.config.dataQualityChecksUrl ?: "https://docs.google.com/spreadsheet/pub?key=0AjNtzhUIIHeNdHJOYk1SYWE4dU1BMWZmb2hiTjlYQlE&single=true&gid=0&output=csv"
        getText(url)
    }

    @Cacheable('longTermCache')
    def JSONArray getLoggerReasons() {
        def url = "${grailsApplication.config.logger.baseUrl}/logger/reasons"
        def jsonObj = getJsonElements(url)
        jsonObj.findAll { !it.deprecated } // skip deprecated reason codes
    }

    @Cacheable('longTermCache')
    def JSONArray getLoggerSources() {
        def url = "${grailsApplication.config.logger.baseUrl}/logger/sources"
        try {
            getJsonElements(url)
        } catch (Exception ex) {
            log.error "Error calling logger service: ${ex.message}", ex
        }
    }

    @Cacheable('longTermCache')
    def JSONArray getAssertionCodes() {
        def url = "${grailsApplication.config.biocache.baseUrl}/assertions/codes"
        return getJsonElements(url)
    }

    /**
     * Generate a Map of image url (key) with image file size (like ls -h) (value)
     *
     * @param images
     * @return
     */
    def Map getImageFileSizeInMb(JSONArray images) {
        Map imageSizes = [:]

        images.each { image ->
            //log.debug "image = ${image}"
            String originalImageUrl = image.alternativeFormats?.imageUrl
            if (originalImageUrl) {
                Long imageSizeInBytes = getImageSizeInBytes(originalImageUrl)
                String formattedImageSize = FileUtils.byteCountToDisplaySize(imageSizeInBytes) // human readable value
                imageSizes.put(originalImageUrl, formattedImageSize)
            }
        }

        imageSizes
    }

    /**
     * Get list of dynamic facets for a given query (Sandbox)
     *
     * @param query
     * @return
     */
    List getDynamicFacets(String query) {
        def url = "${grailsApplication.config.biocache.baseUrl}/upload/dynamicFacets?q=${query}"
        JSONArray facets = getJsonElements(url)
        def dfs = []
        facets.each {
            if (it.name && it.displayName) {
                dfs.add([name: it.name, displayName: it.displayName])
            } // reduce to List of Maps
        }
        dfs
    }

    /**
     * Use HTTP HEAD to determine the file size of a URL (image)
     *
     * @param imageURL
     * @return
     * @throws Exception
     */
    private Long getImageSizeInBytes(String imageURL) throws Exception {
        // encode the path part of the URI - taken from http://stackoverflow.com/a/8962869/249327
        Long imageFileSize = 0l
        try {
            URL url = new URL(imageURL);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
            HttpClient httpClient = new HttpClient()
            HeadMethod headMethod = new HeadMethod(uri.toString())
            httpClient.executeMethod(headMethod)
            String lengthString = headMethod.getResponseHeader("Content-Length")?.getValue()?:'0'
            imageFileSize = Long.parseLong(lengthString)
        } catch (Exception ex) {
            log.error "Error getting image url file size: ${ex}", ex
        }

        return imageFileSize
    }

    /**
     * Perform HTTP GET on a JSON web service
     *
     * @param url
     * @return
     */
    JSONElement getJsonElements(String url) {
        log.debug "(internal) getJson URL = " + url
        def conn = new URL(url).openConnection()
        try {
            conn.setConnectTimeout(10000)
            conn.setReadTimeout(50000)
            return JSON.parse(conn.getInputStream(), "UTF-8")
        } catch (Exception e) {
            def error = "Failed to get json from web service (${url}). ${e.getClass()} ${e.getMessage()}, ${e}"
            log.error error
            throw new RestClientException(error, e)
        }
    }

    /**
     * Perform HTTP GET on a text-based web service
     *
     * @param url
     * @return
     */
    String getText(String url) {
        log.debug "(internal text) getText URL = " + url
        def conn = new URL(url).openConnection()

        try {
            conn.setConnectTimeout(10000)
            conn.setReadTimeout(50000)
            def text = conn.content.text
            return text
        } catch (Exception e) {
            def error = "Failed to get text from web service (${url}). ${e.getClass()} ${e.getMessage()}, ${e}"
            log.error error
            //return null
            throw new RestClientException(error, e) // exception will result in no caching as opposed to returning null
        }
    }

    /**
     * Perform a POST with URL encoded params as POST body
     *
     * @param uri
     * @param postParams
     * @return postResponse (Map with keys: statusCode (int) and statusMsg (String)
     */
    def Map postFormData(String uri, Map postParams) {
        HTTPBuilder http = new HTTPBuilder(uri)
        log.debug "POST (form encoded) to ${http.uri}"
        Map postResponse = [:]

        http.request( Method.POST ) {

            send ContentType.URLENC, postParams

            response.success = { resp ->
                log.debug "POST - response status: ${resp.statusLine}"
                postResponse.statusCode = resp.statusLine.statusCode
                postResponse.statusMsg = resp.statusLine.reasonPhrase
                //assert resp.statusLine.statusCode == 201
            }

            response.failure = { resp ->
                //def error = [error: "Unexpected error: ${resp.statusLine.statusCode} : ${resp.statusLine.reasonPhrase}"]
                postResponse.statusCode = resp.statusLine.statusCode
                postResponse.statusMsg = resp.statusLine.reasonPhrase
                log.error "POST - Unexpected error: ${postResponse.statusCode} : ${postResponse.statusMsg}"
            }
        }

        postResponse
    }

    def JSONElement postJsonElements(String url, String jsonBody) {
        HttpURLConnection conn = null
        def charEncoding = 'UTF-8'
        try {
            conn = new URL(url).openConnection()
            conn.setDoOutput(true)
            conn.setRequestProperty("Content-Type", "application/json;charset=${charEncoding}");
//            conn.setRequestProperty("Authorization", grailsApplication.config.api_key);
//            def user = userService.getUser()
//            if (user) {
//                conn.setRequestProperty(grailsApplication.config.app.http.header.userId, user.userId) // used by ecodata
//                conn.setRequestProperty("Cookie", "ALA-Auth="+java.net.URLEncoder.encode(user.userName, charEncoding)) // used by specieslist
//            }
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), charEncoding)
            wr.write(jsonBody)
            wr.flush()
            def resp = conn.inputStream.text
            log.debug "fileid = ${conn.getHeaderField("fileId")}"
            //log.debug "resp = ${resp}"
            //log.debug "code = ${conn.getResponseCode()}"
            if (!resp && conn.getResponseCode() == 201) {
                // Field guide code...
                log.debug "field guide catch"
                resp = "{fileId: \"${conn.getHeaderField("fileId")}\" }"
            }
            wr.close()
            return JSON.parse(resp?:"{}")
        } catch (SocketTimeoutException e) {
            def error = "Timed out calling web service. URL= ${url}."
            throw new RestClientException(error) // exception will result in no caching as opposed to returning null
        } catch (Exception e) {
            def error = "Failed calling web service. ${e.getMessage()} URL= ${url}." +
                        "statusCode: " +conn?.responseCode?:"" +
                        "detail: " + conn?.errorStream?.text
            throw new RestClientException(error) // exception will result in no caching as opposed to returning null
        }
    }

    /**
     * Standard deep copy implementation
     *
     * Taken from http://stackoverflow.com/a/13155429/249327
     *
     * @param orig
     * @return
     */
    private def deepCopy(orig) {
        def bos = new ByteArrayOutputStream()
        def oos = new ObjectOutputStream(bos)
        oos.writeObject(orig); oos.flush()
        def bin = new ByteArrayInputStream(bos.toByteArray())
        def ois = new ObjectInputStream(bin)
        return ois.readObject()
    }

    JSONElement facetSearch(SearchRequestParams requestParams) {
        def result = applyQualityFiltersToJsonRequest(requestParams) { newParams ->
            newParams.pageSize = 0
            def url = "${grailsApplication.config.biocache.baseUrl}/occurrences/search?${newParams.getEncodedParams()}"
            getJsonElements(url)
        }

        return result
    }

    String facetCSVDownload(SearchRequestParams requestParams) {
        def newParams = convertSearchRequestParamsForDataQualitySettings(requestParams)
        def url = "${grailsApplication.config.biocache.baseUrl}/occurrences/facets/download?${newParams.getEncodedParams()}&count=true&lookup=true"
        def result = getText(url)
        return result
    }

    def getAllOccurrenceFields() {
        def url = "${grailsApplication.config.biocache.baseUrl}/index/fields"
        return getJsonElements(url)?.collect {it.name}
    }

    def getMessagesPropertiesFile() {
        def url = "${grailsApplication.config.biocache.baseUrl}/facets/i18n"

        def map = [:]
        def lineContent
        // split text to get lines
        def lines = getText(url).split("\\r?\\n")
        lines?.each {
            // if not comment
            if (!it.startsWith('#')) {
                lineContent = it.split('=')
                if (lineContent.length == 2) {
                    map[lineContent[0]] = lineContent[1]
                }
            }
        }
        return map
    }

    def getAssertionCodeMap() {
        JSONArray codes = getAssertionCodes() // code <-> name
        Map dataQualityCodes = getAllCodes()  // code -> detail

        // convert to name -> detail
        return codes.findAll{dataQualityCodes.containsKey(String.valueOf(it.code))}.collectEntries{[(it.name) : dataQualityCodes.get(String.valueOf(it.code))]}
    }

    def getAllCodes() {
        Map dataQualityCodes = [:]
        String dataQualityCsv = getDataQualityCsv() // cached
        ICsvListReader listReader = null

        try {
            listReader = new CsvListReader(new StringReader(dataQualityCsv), CsvPreference.STANDARD_PREFERENCE)
            listReader.getHeader(true) // skip the header (can't be used with CsvListReader)
            final CellProcessor[] processors = getProcessors()

            List<Object> dataQualityList
            while ((dataQualityList = listReader.read(processors)) != null) {
                //log.debug("row: " + StringUtils.join(dataQualityList, "|"));
                Map<String, String> dataQualityEls = new HashMap<String, String>();
                if (dataQualityList.get(1) != null) {
                    dataQualityEls.put("name", (String) dataQualityList.get(1));
                }
                if (dataQualityList.get(3) != null) {
                    dataQualityEls.put("description", (String) dataQualityList.get(3));
                }
                if (dataQualityList.get(4) != null) {
                    dataQualityEls.put("wiki", (String) dataQualityList.get(4));
                }
                if (dataQualityList.get(0) != null) {
                    dataQualityCodes.put((String) dataQualityList.get(0), dataQualityEls);
                }
            }
        } finally {
            if (listReader != null) {
                listReader.close();
            }
        }
        dataQualityCodes
    }

    /**
     * CellProcessor method as required by SuperCSV
     *
     * @return
     */
    private static CellProcessor[] getProcessors() {
        final CellProcessor[] processors = [
                null, // col 1
                null, // col 2
                null, // col 3
                null, // col 4
                null, // col 5
                null, // col 6
                null, // col 7
                null, // col 8
                null, // col 9
                null, // col 10
                null, // col 11
                null, // col 12
                null, // col 13
                null, // col 14
                null, // col 15
        ]

        return processors
    }
}