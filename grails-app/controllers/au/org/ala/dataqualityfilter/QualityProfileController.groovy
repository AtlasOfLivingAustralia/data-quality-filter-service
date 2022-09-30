package au.org.ala.dataqualityfilter

import au.org.ala.plugins.openapi.Path
import grails.converters.JSON
import grails.rest.RestfulController
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse

import javax.ws.rs.Produces

import static io.swagger.v3.oas.annotations.enums.ParameterIn.PATH
import static io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY




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

    @Operation(
            method = "GET",
            tags = "data-profiles",
            operationId = "getQualityProfiles",
            summary = "List all quality profiles",
            description = "List all quality profiles",
            parameters = [
                    @Parameter(
                            name = "max",
                            in = QUERY,
                            description = "Maximum results to return",
                            schema = @Schema(implementation = Integer),
                            required = false
                    ),
                    @Parameter(
                            name = "offset",
                            in = QUERY,
                            description = "Offset results by",
                            schema = @Schema(implementation = Integer),
                            required = false
                    ),
                    @Parameter(
                            name = "sort",
                            in = QUERY,
                            description = "Property to sort results by",
                            schema = @Schema(implementation = String),
                            required = false
                    ),
                    @Parameter(
                            name = "order",
                            in = QUERY,
                            description = "Direction to sort results by",
                            schema = @Schema(implementation = String),
                            required = false
                    ),
                    @Parameter(
                            name = "enabled",
                            in = QUERY,
                            description = "Only return enabled profiles",
                            schema = @Schema(implementation = Boolean),
                            required = false
                    ),
                    @Parameter(
                            name = "name",
                            in = QUERY,
                            description = "Search for profiles by name",
                            schema = @Schema(implementation = String),
                            required = false
                    ),
                    @Parameter(
                            name = "shortName",
                            in = QUERY,
                            description = "Search for profiles by short name",
                            schema = @Schema(implementation = String),
                            required = false
                    )
            ],
            responses = [
                    @ApiResponse(
                            description = "List of quality profiles",
                            responseCode = "200",
                            content = [
                                    @Content(
                                            mediaType = "application/json",
                                            array = @ArraySchema(schema = @Schema(implementation = QualityProfile))
                                    )
                            ],
                            headers = [
                                    @Header(name = 'Access-Control-Allow-Headers', description = "CORS header", schema = @Schema(type = "String")),
                                    @Header(name = 'Access-Control-Allow-Methods', description = "CORS header", schema = @Schema(type = "String")),
                                    @Header(name = 'Access-Control-Allow-Origin', description = "CORS header", schema = @Schema(type = "String"))
                            ]
                    )
            ]
    )
    @Path("/api/v1/data-profiles")
    @Produces("application/json")
    def index () {
        render listAllResources(params) as JSON
    }

    @Operation(
            method = "GET",
            tags = "data-profiles",
            operationId = "getQualityProfile",
            summary = "Retrieve a single quality profile",
            description = "Retrieve a single quality profile",
            parameters = [
                    @Parameter(
                            name = "id",
                            in = PATH,
                            description = "The id or short name for the quality profile or default for the default profile",
                            schema = @Schema(implementation = String),
                            required = true
                    )
            ],
            responses = [
                    @ApiResponse(
                            description = "List of quality profiles",
                            responseCode = "200",
                            content = [
                                    @Content(
                                            mediaType = "application/json",
                                            schema  = @Schema(implementation = QualityProfile)
                                    )
                            ],
                            headers = [
                                    @Header(name = 'Access-Control-Allow-Headers', description = "CORS header", schema = @Schema(type = "String")),
                                    @Header(name = 'Access-Control-Allow-Methods', description = "CORS header", schema = @Schema(type = "String")),
                                    @Header(name = 'Access-Control-Allow-Origin', description = "CORS header", schema = @Schema(type = "String"))
                            ]
                    )
            ]
    )
    @Path("/api/v1/data-profiles/{id}")
    @Produces("application/json")
    def show() {
        render queryForResource(params.id) as JSON
    }
}
