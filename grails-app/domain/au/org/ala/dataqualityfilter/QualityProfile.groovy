package au.org.ala.dataqualityfilter

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode(includes = ['name', 'shortName'])
@ToString(includes = ['enabled', 'name', 'shortName', 'isDefault'])
class QualityProfile {

    Long id

    String name
    String shortName
    String description

    String contactName
    String contactEmail

    boolean enabled
    boolean isDefault

    Long displayOrder

    Date dateCreated
    Date lastUpdated

    static hasMany = [categories: QualityCategory]

    static constraints = {
        name unique: true, blank: false, nullable: false
        shortName unique: true, blank: false, nullable: false
        contactEmail blank: true, nullable: true

        // TODO remove after db updated
        contactName blank: true, nullable: true
        description blank: true, nullable: true
    }

    static mapping = {
        cache true
        name type: 'text'
        shortName type: 'text'
        description type: 'text'
        contactName type: 'text'
        contactEmail type: 'text'
        enabled defaultValue: 'true', index: 'quality_profile_enabled_idx'
        categories sort: 'displayOrder'
        sort 'displayOrder'
    }

    static namedQueries = {
        selectMaxDisplayOrder {
            projections {
                max('displayOrder')
            }
        }
    }
}
