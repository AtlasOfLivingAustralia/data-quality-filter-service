package au.org.ala.dataqualityfilter

import grails.rest.RestfulController
import io.swagger.annotations.Api
import io.swagger.annotations.ApiImplicitParam
import io.swagger.annotations.ApiImplicitParams
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses

import static javax.servlet.http.HttpServletResponse.SC_OK

@Api(value = "/api/v1/", tags = ["data-profiles"], description = "Data Quality RESTful API for Quality Profiles")
class QualityProfileController extends RestfulController<QualityProfile> {

    static responseFormats = ['json']

    def qualityService

    QualityProfileController() {
        super(QualityProfile, true)
    }

    @Override
    protected QualityProfile queryForResource(Serializable id) {
        return qualityService.findProfileById(id)
    }

    @Override
    protected List<QualityProfile> listAllResources(Map params) {
        return qualityService.queryProfiles(params)
    }

    @ApiOperation(
            value = "List all quality profiles",
            nickname = "data-profiles",
            produces = "application/json",
            httpMethod = "GET"
    )
    @ApiResponses([
            @ApiResponse(code = SC_OK, message = "OK", response = QualityProfile, responseContainer = "List")
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = "max", paramType = "query", required = false, value = "Maximum results to return", dataType = 'integer'),
            @ApiImplicitParam(name = "offset", paramType = "query", required = false, value = "Offset results by", dataType = 'integer'),
            @ApiImplicitParam(name = "sort", paramType = "query", required = false, value = "Property to sort results by", dataType = 'string'),
            @ApiImplicitParam(name = "order", paramType = "query", required = false, value = "Direction to sort results by", dataType = 'string'),
            @ApiImplicitParam(name = "enabled", paramType = "query", required = false, value = "Only return enabled profiles", dataType = 'boolean'),
            @ApiImplicitParam(name = "name", paramType = "query", required = false, value = "Search for profiles by name", dataType = 'string'),
            @ApiImplicitParam(name = "shortName", paramType = "query", required = false, value = "Search for profiles by short name", dataType = 'string'),
            @ApiImplicitParam(name = "userId", paramType = "query", required = false, value = "the userId used to search private profiles", dataType = 'string')
    ])
    def index(Integer max) {
        super.index(max)
    }

    @ApiOperation(
            value = "Retrieve a single quality profile",
            nickname = "data-profiles/{id}",
            produces = "application/json",
            httpMethod = "GET"
    )
    @ApiResponses([
            @ApiResponse(code = SC_OK, message = "OK", response = QualityProfile)
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = "id", paramType = "path", required = false, value = "The id or short name for the quality profile", dataType = 'string')
    ])
    def show() {
        super.show()
    }
}
