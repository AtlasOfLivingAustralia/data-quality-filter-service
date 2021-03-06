package au.org.ala.dataqualityfilter

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import grails.validation.ValidationErrors
import grails.validation.ValidationException
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import spock.lang.Specification

class AdminDataQualityFiltersControllerSpec extends Specification implements ControllerUnitTest<AdminDataQualityController>, DataTest {

    def setupSpec() {
        mockDomains(QualityCategory, QualityFilter, QualityProfile)
    }

    def setup() {
        controller.qualityService = Mock(QualityService)
        controller.webServicesService = Mock(WebServicesService)
    }

    def 'test filters'() {
        setup:
        QualityProfile qp1 = new QualityProfile(name: 'name', shortName: 'name', enabled: true, isDefault: true, displayOrder: 1).save(flush: true)

        QualityCategory qc1 = new QualityCategory(name: 'name', label: 'label', description: 'description', qualityProfile: qp1, displayOrder: 1).save(flush: true)
        QualityCategory qc2 = new QualityCategory(name: 'name2', label: 'label2', description: 'description', qualityProfile: qp1, displayOrder: 2).save(flush: true)

        when:
        params.id = qp1.id
        controller.filters()

        then:
        1 * controller.webServicesService.getAllOccurrenceFields() >> []
        1 * controller.qualityService.getEnabledFiltersByLabel(qp1.shortName) >> [label: '', label2: '']
        model.qualityCategoryInstanceList == [qc1, qc2]
        model.qualityFilterStrings == [label: '', label2: '']
    }

    def 'test saveQualityCategory'() {
        setup:
        QualityProfile qp1 = new QualityProfile(name: 'name', shortName: 'name', enabled: true, isDefault: true, displayOrder: 1).save(flush: true)

        QualityCategory qc1 = new QualityCategory(name: 'name', label: 'label', description: 'description', qualityProfile: qp1, displayOrder: 1)

        setupTokens('/admin/dataQuality/saveQualityCategory')

        when:
        controller.saveQualityCategory(qc1)

        then:
        1 * controller.qualityService.createOrUpdateCategory(qc1) >> qc1
        response.redirectedUrl == '/filters/' + qp1.id
    }

    def 'test saveQualityCategory failure'() {
        setup:
        QualityProfile qp1 = new QualityProfile(name: 'name', shortName: 'name', enabled: true, isDefault: true, displayOrder: 1).save(flush: true)

        QualityCategory qc1 = new QualityCategory(name: 'name', label: 'label', description: 'description', qualityProfile: qp1, displayOrder: 1)
        ValidationErrors ve = new ValidationErrors(qc1, 'qualityCategory')

        setupTokens('/saveQualityCategory')

        when:
        controller.saveQualityCategory(qc1)

        then:
        1 * controller.qualityService.createOrUpdateCategory(qc1) >> { throw new ValidationException('msg', ve) }
        flash.errors == ve
        response.redirectedUrl == '/filters/' + qp1.id
    }

    def 'test enableQualityCategory'() {
        setup:
        QualityProfile qp1 = new QualityProfile(name: 'name', shortName: 'name', enabled: true, isDefault: true, displayOrder: 1).save(flush: true)

        QualityCategory qc1 = new QualityCategory(name: 'name', label: 'label', description: 'description', enabled: true, qualityProfile: qp1, displayOrder: 1).save(flush: true)
        request.addParameter('id', "${qc1.id}")
        request.addParameter('enabled', "false")

        setupTokens('/enableQualityCategory')


        when:
        controller.enableQualityCategory()

        then:
        qc1.enabled == false
        response.redirectedUrl == '/filters/' + qp1.id

    }

    def 'test deleteQualityCategory'() {
        setup:
        QualityProfile qp1 = new QualityProfile(name: 'name', shortName: 'name', enabled: true, isDefault: true, displayOrder: 1).save(flush: true)

        QualityCategory qc1 = new QualityCategory(name: 'name', label: 'label', description: 'description', enabled: true, qualityProfile: qp1, displayOrder: 1).save(flush: true)

        setupTokens('/deleteQualityCategory')

        when:
        controller.deleteQualityCategory(qc1)

        then:
        1 * controller.qualityService.deleteCategory(qc1)
        response.redirectedUrl == '/filters/' + qp1.id
    }

    def 'test saveQualityFilter'() {
        setup:
        QualityProfile qp1 = new QualityProfile(name: 'name', shortName: 'name', enabled: true, isDefault: true, displayOrder: 1).save(flush: true)

        QualityCategory qc1 = new QualityCategory(name: 'name', label: 'label', description: 'description', qualityProfile: qp1, displayOrder: 1).save(flush: true)
        QualityFilter qf1 = new QualityFilter(description: 'desc', filter: 'filter', qualityCategory: qc1, displayOrder: 1)

        setupTokens('/saveQualityFilter')

        when:
        controller.saveQualityFilter(qf1)

        then:
        1 * controller.qualityService.createOrUpdateFilter(qf1) >> qf1
        response.redirectedUrl == '/filters/' + qp1.id
    }

    def 'test saveQualityFilter failure'() {
        setup:
        QualityProfile qp1 = new QualityProfile(name: 'name', shortName: 'name', enabled: true, isDefault: true, displayOrder: 1).save(flush: true)

        QualityCategory qc1 = new QualityCategory(name: 'name', label: 'label', description: 'description', qualityProfile: qp1, displayOrder: 1).save(flush: true)
        QualityFilter qf1 = new QualityFilter(description: 'desc', filter: 'filter', qualityCategory: qc1, displayOrder: 1)
        ValidationErrors ve = new ValidationErrors(qf1, 'qualityFilter')

        setupTokens('/saveQualityFilter')

        when:
        controller.saveQualityFilter(qf1)

        then:
        1 * controller.qualityService.createOrUpdateFilter(qf1) >> { throw new ValidationException('msg', ve) }
        flash.errors == ve
        response.redirectedUrl == '/filters/' + qp1.id
    }

//    @Ignore('fails only in UT for unknown reason')
    def 'test saveQualityFilter failure 2'() {
        setup:
        QualityProfile qp1 = new QualityProfile(name: 'name', shortName: 'name', enabled: true, isDefault: true, displayOrder: 1).save(flush: true)

        QualityCategory qc1 = new QualityCategory(name: 'name', label: 'label', description: 'description', qualityProfile: qp1, displayOrder: 1).save(flush: true)
        QualityFilter qf1 = new QualityFilter(description: 'desc', filter: 'filter', qualityCategory: qc1, displayOrder: 1)
        ValidationErrors ve = new ValidationErrors(qc1, 'qualityCategory')

        setupTokens('/saveQualityFilter')

        when:
        controller.saveQualityFilter(qf1)

        then:
        1 * controller.qualityService.createOrUpdateFilter(qf1) >> { throw new IllegalStateException('msg') }
        response.status == 400
    }

    def 'test deleteQualityFilter'() {
        setup:
        QualityProfile qp1 = new QualityProfile(name: 'name', shortName: 'name', enabled: true, isDefault: true, displayOrder: 1).save(flush: true)

        QualityCategory qc1 = new QualityCategory(name: 'name', label: 'label', description: 'description', enabled: true, qualityProfile: qp1, displayOrder: 1).save(flush: true)
        QualityFilter qf1 = new QualityFilter(description: 'desc', filter: 'filter', qualityCategory: qc1, displayOrder: 1).save(flush: true)
        request.addParameter('id', "${qf1.id}")
        request.addParameter('profileId', "${qp1.id}")

        setupTokens('/deleteQualityFilter')

        when:
        controller.deleteQualityFilter()

        then:
        1 * controller.qualityService.deleteFilter(qf1.id)
        response.redirectedUrl == '/filters/' + qp1.id
    }

    def 'test enableQualityFilter'() {
        setup:
        QualityProfile qp1 = new QualityProfile(name: 'name', shortName: 'name', enabled: true, isDefault: true, displayOrder: 1).save(flush: true)

        QualityCategory qc1 = new QualityCategory(name: 'name', label: 'label', description: 'description', enabled: true, qualityProfile: qp1, displayOrder: 1).save(flush: true)
        QualityFilter qf1 = new QualityFilter(description: 'desc', filter: 'filter', enabled: true, qualityCategory: qc1, displayOrder: 1).save(flush: true)
        request.addParameter('id', "${qf1.id}")
        request.addParameter('enabled', "false")

        setupTokens('/enableQualityFilter')

        when:
        controller.enableQualityFilter()

        then:
        qf1.enabled == false
        response.redirectedUrl == '/filters/' + qp1.id
    }

    private def setupTokens(String path) {
        def tokenHolder = SynchronizerTokensHolder.store(session)

        params[SynchronizerTokensHolder.TOKEN_URI] = path
        params[SynchronizerTokensHolder.TOKEN_KEY] = tokenHolder.generateToken(params[SynchronizerTokensHolder.TOKEN_URI])

    }
}
