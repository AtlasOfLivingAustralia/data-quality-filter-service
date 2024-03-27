package au.org.ala.dataqualityfilter

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode(includes = ['enabled', 'name', 'label', 'description'])
@ToString(includes = ['enabled', 'name', 'label', 'description'])
class QualityCategory {

    Long id

    boolean enabled = true

    String name
    String label
    String description
    Long displayOrder

    String inverseFilter

    Date dateCreated
    Date lastUpdated

    static belongsTo = [ qualityProfile: QualityProfile ]
    static hasMany = [ qualityFilters: QualityFilter ]

    static constraints = {
        qualityProfile nullable: true // TODO remove once migrated
        name unique: ['qualityProfile']
        label unique: ['qualityProfile']
        description blank: true, nullable: true
        inverseFilter nullable: true
    }

    static mapping = {
        cache true
        enabled defaultValue: 'true', index: 'quality_category_enabled_idx'
        name type: 'text'
        label type: 'text'
        description type: 'text'
        dateCreated index: 'quality_category_date_created_idx'
        qualityFilters sort: 'displayOrder'
        sort 'displayOrder'
        inverseFilter type: 'text'
    }

    static namedQueries = {
        selectMaxDisplayOrder {
            projections {
                max('displayOrder')
            }
        }
    }
}
