<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <title>Admin Functions | ${grailsApplication.config.skin.orgNameLong}</title>
    <asset:javascript src="jquery.tablednd.js" />
    <asset:javascript src="jquery.js" />
    <asset:javascript src="bootbox/bootbox.min.js" />
    <asset:stylesheet src="admin.css" />
    <script type="text/javascript">
        var options = "${options.join(',')}"
    </script>
    <style>
    .smallpadding {
        padding-left: 5px;
        padding-right: 5px;
    }
    </style>
</head>
<body>
<div class="row">
    <div class="col-md-12">
        <div id="breadcrumb">
            <ol class="breadcrumb">
                <li><a href="${g.createLink(uri:"/")}">Home</a> <span class=" icon icon-arrow-right"></span></li>
                <li class="active">${profile.name} Data Quality Filters</li>
            </ol>
        </div>
    </div>
</div>
%{-- escape from container-fluid --}%
<div class="container">
<div class="row">
    <div class="col-md-12">
        <h1>${profile.name} Data Quality Filters</h1>

        <div class="well" id="filtersboard">
            <ul></ul>
        </div>
        <g:if test="${flash.message}">
            <div class="alert alert-warning">
                <p>${flash.message}</p>
            </div>
        </g:if>
        <g:hasErrors>
            <div class="alert alert-danger">
                <ul>
                    <g:eachError var="error">
                        <li <g:if test="${error in org.springframework.validation.FieldError}">data-field-id="${error.field}"</g:if>><g:message error="${error}"/></li>
                    </g:eachError>
                </ul>
            </div>
        </g:hasErrors>

        <p>
            <button class="btn btn-primary" id="add-category" data-toggle="modal" data-target="#add-category-modal"><i class="fa fa-plus"></i> Add category</button>
        </p>

    </div>
    <div class="col-md-12">
        <table class="table table-responsive categoriestable" style="table-layout:fixed;">
            <g:if test="${qualityCategoryInstanceList != null && qualityCategoryInstanceList.size() > 0}">
                <g:each in="${qualityCategoryInstanceList.sort{it.displayOrder}}" var="category">
                <tr id="${'category' + category.id}" data-curdisplayorder="${category.displayOrder}">
                    <td style="border:none">
                        <div class="panel ${category.enabled ? 'panel-default' : 'panel-warning'} panel-category">
                            <div class="panel-heading">
                                <g:form action="deleteQualityCategory" useToken="true" class="form-inline pull-right" data-confirmation="${category.qualityFilters.size() > 0}"><g:hiddenField name="id" value="${category.id}"/><button type="submit" class="btn btn-xs btn-danger">&times;</button></g:form>
                                <h3 class="panel-title">
                                    <img src="${assetPath(src: 'menu.png')}" class="dragHandleCategory" data-toggle="tooltip" title="drag/drop to re-order categories" style="display: inline"></img>
                                    <g:form class="form-inline" style="display: inline-block;" useToken="true" action="enableQualityCategory">
                                        <g:hiddenField name="id" value="${category.id}"/>
                                        <label class="sr-only">Enabled</label>
                                        <g:checkBox name="enabled" value="${category.enabled}" />
                                    </g:form>
                                    <span class="panel-title-ro">${category.name} (${category.label}) <button class="btn btn-xs btn-default btn-edit-category"><i class="fa fa-edit"></i></button></span>
                                    <span class="panel-title-rw hidden">
                                        <g:form action="saveQualityCategory" useToken="true" class="form-inline">
                                            <g:hiddenField name="id" value="${category.id}"/>
                                            <g:hiddenField name="version" value="${category.version}"/>
                                            <g:hiddenField name="description" value="${category.description}" />
                                            <g:hiddenField name="inverse-filter" value="${category.inverseFilter}" />
                                            <div class="form-group">
                                                <label for="name">Name</label>
                                                <g:textField class="form-control" name="name" value="${category.name}" />
                                            </div>
                                            <div class="form-group">
                                                <label for="label">Label</label>
                                                <g:textField class="form-control" name="label" value="${category.label}" />
                                            </div>
                                            <button type="submit" class="btn btn-sm btn-success"><i class="fa fa-save"></i></button>
                                            <button type="reset" class="btn btn-sm btn-default"><i class="fa fa-close"></i></button>
                                        </g:form>
                                    </span>
                                </h3>
                            </div>
                            <div class="panel-body">
                                <span class="category-description-ro">
                                    <label for="${category.id + '-description'}">Description</label>
                                    <p class="category-description" id="${category.id + '-description'}">${category.description}</p>
                                    <label for="${category.id + '-inverse-filter'}">Inverse Filter (required if any "Filter Value" is complex. e.g. has a bracket)</label>
                                    <p class="category-inverse-filter" id="${category.id + '-inverse-filter'}">${category.inverseFilter}</p>
                                    <g:if test="${!category.inverseFilter && category.qualityFilters.find{it.filter.contains(" AND ") || it.filter.contains(" OR ") || it.filter.contains("(") }}">
                                        <div class="alert alert-danger">
                                            <p>Error: inverseFilter required</p>
                                        </div>
                                    </g:if>
                                    <button class="btn btn-default"><i class="fa fa-edit"></i></button>
                                </span>
                                <span class="category-description-rw hidden">
                                    <g:form action="saveQualityCategory" useToken="true">
                                        <g:hiddenField name="id" value="${category.id}"/>
                                        <g:hiddenField name="version" value="${category.version}"/>
                                        <g:hiddenField name="name" value="${category.name}"/>
                                        <g:hiddenField name="label" value="${category.label}"/>
                                        <label for="${category.id + '-description-textarea'}">Description</label>
                                        <g:textArea class="form-control" name="description" id="${category.id + '-description-textarea'}" value="${category.description}" />
                                        <label for="${category.id + '-inverse-filter-textarea'}">Inverse Filter (required if any "Filter Value" is complex. e.g. has a bracket)</label>
                                        <g:textArea class="form-control" name="inverseFilter" id="${category.id + '-inverse-filter-textarea'}" value="${category.inverseFilter}" />
                                        <button type="submit" class="btn btn-success"><i class="fa fa-save"></i></button>
                                        <button type="reset" class="btn btn-default"><i class="fa fa-close"></i></button>
                                    </g:form>
                                </span>
                            </div>
                            <table id="${category.id}" data-profileid="${profile.id}" data-categorylabel="${category.label}" class="table table-responsive filterstable" style="table-layout:fixed;">
                                <g:each in="${category.qualityFilters.sort{it.displayOrder}}" var="filter">
                                <tr id="${'filter' + filter.id}" class="${filter.enabled ? 'bg-default' : 'bg-warning'}" data-curdisplayorder="${filter.displayOrder}" data-filtervalue="${filter.filter}">
                                    <td style="vertical-align: middle; width:15px"><img src="${assetPath(src: 'menu.png')}" class="dragHandle" data-toggle="tooltip" title="drag/drop to re-order filters"></img></td>
                                    <td>
                                        <g:form useToken="true" action="enableQualityFilter">
                                            <g:hiddenField name="id" value="${filter.id}"/>
                                            <div class="smallpadding">
                                                <label form-control for="${filter.id + '-enabled'}">Enable filter&nbsp;</label><g:checkBox name="enabled" id="${filter.id + '-enabled'}" class='filter-enabled' value="${filter.enabled}" />
                                            </div>
                                        </g:form>
                                        <g:form action="saveQualityFilter" class="saveFilter" useToken="true">
                                            <g:hiddenField name="id" value="${filter.id}"/>
                                            <g:hiddenField name="version" value="${filter.version}"/>
                                            <g:hiddenField name="qualityCategory" value="${category.id}" />
                                            <div class="col-md-12" style="padding-left: 0">
                                                <div class="col-md-3 smallpadding">
                                                    <label for="${filter.id + '-description'}">Filter Description</label>
                                                    <button type="button" class="btn btn-xs btn-default btn-load-filter-desc" title="Load field description"><i class="fa fa-download"></i></button>
                                                </div>
                                                <div class="col-md-3 smallpadding">
                                                    <label for="${filter.id + '-key'}">Filter Key</label>
                                                </div>
                                                <div class="col-md-2 smallpadding">
                                                    <label for="${filter.id + '-value'}">Filter Value</label>
                                                </div>
                                                <div class="col-md-2 smallpadding">
                                                    <label for="${filter.id + '-generated'}">Generated Filter</label>
                                                </div>
                                            </div>

                                            <div class="col-md-12 current-filter filter-row" style="padding-left: 0" data-fq="${filter.filter}">
                                                <div class="col-md-3 smallpadding">
                                                    <g:textArea class="form-control filterDescription" name="description" id="${filter.id + '-description'}" value="${filter.description}" data-orig="${filter.description}" style="width: 100%" oninput="resizeTextArea(this)"/>
                                                </div>
                                                <div class="col-md-3 smallpadding" style="display: flex;">
                                                    <select class="form-control exclude" style="width: 30%">
                                                        <option value="Include">Include</option>
                                                        <option value="Exclude">Exclude</option>
                                                    </select>
                                                    <g:select class="form-control filterKey" name="filterKey" id="${filter.id + '-key'}" from="${options}" value="abcd_type_status" noSelection="['':'']" style="width: 70%"/>
                                                </div>
                                                <div class="col-md-2 smallpadding">
                                                    <g:textField class="form-control filterValue" name="filterValue" id="${filter.id + '-value'}" style="width: 100%"/>
                                                </div>
                                                <div class="col-md-2 smallpadding">
                                                    <g:textField class="form-control filter" name="filter" id="${filter.id + '-generated'}" value="${filter.filter}" data-orig="${filter.filter}" readonly="readonly" style="width: 100%"/>
                                                </div>
                                                <div class="col-md-2 smallpadding">
                                                    <button type="submit" class="btn btn-sm btn-success"><i class="fa fa-save"></i></button>
                                                    <button type="reset" class="btn btn-sm btn-default"><i class="fa fa-refresh"></i></button>
                                                    <button type="submit" form="${filter.id}" class="btn btn-sm btn-danger" ><i class="fa fa-trash"></i></button>
                                                </div>
                                            </div>
                                        </g:form>
                                        <g:form class="form-inline" name="${filter.id}" action="deleteQualityFilter" useToken="true" style="display: inline-block;" method="post">
                                            <g:hiddenField name="id" value="${filter.id}"/>
                                            <g:hiddenField name="profileId" value="${category.qualityProfile.id}" />
                                        </g:form>
                                    </td>
                                    <td style="display: none">
                                        <g:form class="updateFilterDisplayOrder" useToken="true">
                                            <g:hiddenField name="id" value="${filter.id}"></g:hiddenField>
                                        </g:form>
                                    </td>
                                </tr>
                                </g:each>
                                <tr class="nodrag nodrop">
                                    <td style="width:15px"></td>
                                    <td>
                                        <g:form action="saveQualityFilter" class="saveFilter" useToken="true">
                                            <g:hiddenField name="qualityCategory" value="${category.id}" />
                                            <div class="col-md-12" style="padding-left: 0">
                                                <div class="col-md-3 smallpadding">
                                                    <label for="${category.id + '-description'}">Filter Description</label>
                                                    <button type="button" class="btn btn-xs btn-default btn-load-filter-desc" title="Load field description"><i class="fa fa-download"></i></button>
                                                </div>
                                                <div class="col-md-3 smallpadding">
                                                    <label for="${category.id + '-key'}">Filter Key</label>
                                                </div>
                                                <div class="col-md-2 smallpadding">
                                                    <label for="${category.id + '-value'}">Filter Value</label>
                                                </div>
                                                <div class="col-md-2 smallpadding">
                                                    <label for="${category.id + '-generated'}">Generated Filter</label>
                                                </div>
                                            </div>
                                            <div class="col-md-12 new-filter filter-row" style="padding-left: 0">
                                                <div class="col-md-3 smallpadding">
                                                    <g:textArea class="form-control filterDescription" name="description" id="${category.id + '-description'}" placeholder="Filter Description" style="width: 100%" oninput="resizeTextArea(this)"/>
                                                </div>
                                                <div class="col-md-3 smallpadding" style="display: flex">
                                                    <select class="form-control exclude" from="" style="width: 30%">
                                                        <option value="Include" selected="selected">Include</option>
                                                        <option value="Exclude">Exclude</option>
                                                    </select>
                                                    <g:select class="form-control filterKey" name="filterKey" id="${category.id + '-key'}" style="width: 70%" from="${options}" noSelection="['':'']"/>
                                                </div>
                                                <div class="col-md-2 smallpadding">
                                                    <g:textField class="form-control filterValue" name="filterValue" id="${category.id + '-value'}" placeholder="Filter value" style="width: 100%"/>
                                                </div>
                                                <div class="col-md-2 smallpadding">
                                                    <g:textField class="form-control filter" name="filter" id="${category.id + '-generated'}" placeholder="Generated Filter" readonly="readonly" style="width: 100%"/>
                                                </div>
                                                <div class="col-md-2 smallpadding">
                                                    <button type="submit" class="btn btn-sm btn-success"><i class="fa fa-plus"></i></button>
                                                    <button type="reset" class="btn btn-sm btn-warning hidden"><i class="fa fa-close"></i></button>
                                                </div>
                                            </div>
                                        </g:form>
                                    </td>
                                </tr>
                            </table>
                        </div>
                     </td>
                    <td style="display: none">
                        <g:form class="updateCategoryDisplayOrder" useToken="true">
                            <g:hiddenField name="id" value="${category.id}"></g:hiddenField>
                        </g:form>
                    </td>
                </tr>
                </g:each>
            </g:if>
        </table>
    </div>
</div>
<div id="add-category-modal" class="modal fade" tabindex="-1" role="dialog">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                <h4 class="modal-title">New Data Quality Category</h4>
            </div>
            <div class="modal-body">
                <g:form name="add-category-form" useToken="true" action="saveQualityCategory" method="POST">
                    <g:hiddenField name="qualityProfile" value="${profile.id}" />
                    <div class="form-group">
                        <label for="name">Category name</label>
                        <g:textField name="name" placeholder="Outliers" class="form-control" />
                    </div>
                    <div class="form-group">
                        <label for="name">Label</label>
                        <g:textField name="label" placeholder="short-label-for-ui" class="form-control" />
                    </div>
                    <div class="form-group">
                        <label for="description">Category description</label>
                        <g:textArea name="description" placeholder="Lorum ipsum..." class="form-control" />
                    </div>
                </g:form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                <button type="submit" form="add-category-form" class="btn btn-primary" id="add-category-save">Save category</button>
            </div>
        </div><!-- /.modal-content -->
    </div><!-- /.modal-dialog -->
</div><!-- /.modal -->
</div>
</body>
<asset:script type="text/javascript">
    $(document).ready(function() {
        setControlValues();
        updateFilters();

        $(".filterstable").tableDnD({
            onDragClass: 'bg-info',
            onDragStop : handleFilterstop,
            dragHandle: ".dragHandle"
        })

        $(".categoriestable").tableDnD({
            onDragClass: 'bg-info',
            onDragStop: handleCategoryStop,
            dragHandle: ".dragHandleCategory"
        })
    });

    function updateFilters() {
        var ul = $('#filtersboard ul')
        // clear content
        $(ul).empty();

        // go through each category (table)
        $('.filterstable').each(function() {
            var categotyEnabled = $(this).closest('.panel').find('*[name=enabled]').first().is(':checked')
            if (categotyEnabled) {
                // get category label
                var label = $(this).attr('data-categorylabel')
                var enabledFilters = []
                // go through each filter in this category
                $(this).children('tbody').children('tr').not(':last').each(function() {
                    // if filter is enabled
                    if ($(this).find('.filter-enabled').is(":checked")) {
                        enabledFilters.push($(this).attr('data-filtervalue'))
                    }
                })

                if (enabledFilters.length > 0) {
                    $(ul).append('<li>' + label + '<code> fq=' + enabledFilters.join(' AND ') + '</code></li>');
                }
            }
        })
    }

    // re-calculate display orders for profiles
    function handleFilterstop(table, row) {
        var filterids = [];
        var orig_displayorders = [];
        // get filter ids (top down order) and old orders (except last row)
        $(table).children("tbody").children("tr").not(":last").each(function() {
            var filterid = $(this).attr('id').substring(6);
            filterids.push(filterid)
            orig_displayorders.push(parseInt($(this).attr('data-curdisplayorder')));
        });

        var new_displayorders = orig_displayorders.slice();
        new_displayorders.sort(function(a, b) {
            return a - b;
        })

        var orderchanged = false;
        for (var i = 0; i < filterids.length; i++) {
            // update filter if display order changed
            if (orig_displayorders[i] !== new_displayorders[i]) {
                orderchanged = true;

                var form = $("#filter" + filterids[i]).find('form[class=updateFilterDisplayOrder]');
                var formData = $(form).serializeArray();
                formData.push({'name':'displayOrder', 'value': new_displayorders[i]});

                $.ajax({
                    type: "POST",
                    url: "${g.createLink(controller: 'adminDataQuality', action: 'saveQualityFilter')}",
                    data: formData,
                    dataType: 'json',
                    accepts: {
                        text: 'text/plain'
                    }
                }).done(function (data) {
                    if (data) {
                        $(table).find('#filter' + data.filter.id).attr('data-curdisplayorder', data.filter.displayOrder);
                        // update token so after each request the form has a new token
                        var form = $('#filter' + data.filter.id).find('form[class=updateFilterDisplayOrder]');
                        $(form).find('input[name=SYNCHRONIZER_TOKEN]').val(data.token);
                    }
                });
            }
        }

        // update filters displayed
        if (orderchanged) {
            updateFilters();
        }
    }

    function handleCategoryStop(table, row) {
        var categoryids = [];
        var orig_displayorders = [];
        // get category ids (top down order) and old orders
        $(table).children("tbody").children("tr").each(function() {
            // category id in the form 'category12'
            categoryids.push($(this).attr('id').substring(8))
            orig_displayorders.push(parseInt($(this).attr('data-curdisplayorder')));
        });

        var new_displayorders = orig_displayorders.slice();
        new_displayorders.sort(function(a, b) {
            return a - b;
        })

        var orderchanged = false;
        for (var i = 0; i < categoryids.length; i++) {
            // update category if display order changed
            if (orig_displayorders[i] !== new_displayorders[i]) {
                orderchanged = true;

                var form = $("#category" + categoryids[i]).find('form[class=updateCategoryDisplayOrder]');
                var formData = $(form).serializeArray();
                formData.push({'name':'displayOrder', 'value': new_displayorders[i]});
                $.ajax({
                    type: "POST",
                    url: "${g.createLink(controller: 'adminDataQuality', action: 'saveQualityCategory')}",
                    data: formData,
                    dataType: 'json',
                    accepts: {
                        text: 'text/plain'
                    }
                }).done(function (data) {
                    if (data) {
                        $(table).find('#category' + data.category.id).attr("data-curdisplayorder", data.category.displayOrder);
                        // update token so after each request the form has a new token
                        var form = $("#category" + data.category.id).find('form[class=updateCategoryDisplayOrder]');
                        $(form).find('input[name=SYNCHRONIZER_TOKEN]').val(data.token);
                    }
                });
            }
        }

        // update filters displayed
        if (orderchanged) {
            updateFilters();
        }
    }

    $(".filterstable tr").hover(function() {
       $(this.cells[0]).find('.dragHandle').css( {'cursor':'move'});
    });

    $(".categoriestable tr").hover(function() {
       $(this.cells[0]).find('.dragHandleCategory').css( {'cursor':'move'});
    });

    $('.saveFilter').on('submit', function(e) {
        var filterDescription = $(this).find('.filterDescription').val().trim();

        if (filterDescription.length === 0) {
            alert("filter description can't be empty");
            e.preventDefault();
            return;
        }

        var filterValue = $(this).find('.filterValue').val().trim();
        if (filterValue.length === 0) {
            alert("filter value can't be empty");
            e.preventDefault();
            return;
        }

        $(this).submit();
    });

    function resizeTextArea(field) {
        field.style.height = Math.max(field.clientHeight, field.scrollHeight) + "px"
        // show the reset button
        $(field).closest('form').find('button[type=reset]').removeClass('hidden');
    }

    // get all the filed names
    var optionset = new Set(options.split(','))

    function setControlValues() {
        // populate fqs
        var currentFilters = $('.current-filter');
        $.each(currentFilters, function(i, el) {
            setFQGroup(el);
        })
    }

    function setFQGroup(fg) {
        var fq = String($(fg).data('fq'));
        var exclude = fq.length > 0 && fq[0] === '-';
        if (exclude) {
            fq = fq.substr(1);
        }

        var count = (fq.match(/:/g)||[]).length;
        var filterKey, filterValue;
        if (count === 1) {
            if (fq.length > 0) {
                var idx = fq.indexOf(':');
                filterKey = fq.substr(0, idx);

                // if it's a valid key
                if (optionset.has(filterKey)) {
                    filterValue = fq.substr(idx + 1);
                } else {
                    filterKey = '';
                    filterValue = fq;
                }
            }
        } else {
            filterKey = '';
            filterValue = fq;
        }
        $('.exclude', $(fg)).val(exclude ? 'Exclude' : 'Include');
        $('.filterKey', $(fg)).val(filterKey);
        $('.filterValue', $(fg)).val(filterValue);
    }

    // confirm delete a category with filters
    $('form[data-confirmation=true]').on('submit', function(e) {
        var $this = $(this);
        if (!confirm("This category has filters defined.  Are you sure you want to delete it?")) { // TODO bootbox
            e.preventDefault();
            return false;
        }
    });
    // submit add category form using button outside form element
    var $categoryForm = $('#add-category-form');
    // $('#add-category-save').on('click', function(e) {
    //    $categoryForm.submit();
    // });
    // default category label
    $categoryForm.find('input[name=name]').on('change', function(e) {
        var $label = $categoryForm.find('input[name=label]');
        if (!$label.val()) {
            $label.val($(this).val().toLowerCase().replace(' ', '-'));
        }
    });
    // Edit category title / label
    $('.panel-title-ro .btn').on('click', function(e) {
        var $this = $(this);
        var $title = $this.closest('.panel-title');
        $title.find('.panel-title-ro').addClass('hidden');
        $title.find('.panel-title-rw').removeClass('hidden');
    });
    // Reset category title / label
    $('.panel-title-rw button[type=reset]').on('click', function(e) {
        var $this = $(this);
        var $title = $this.closest('.panel-title');
        $title.find('.panel-title-ro').removeClass('hidden');
        $title.find('.panel-title-rw').addClass('hidden');
    });
    // Edit category description
    $('.category-description-ro .btn').on('click', function(e) {
        var $this = $(this);
        var $body = $this.closest('.panel-body');
        $body.find('.category-description-ro').addClass('hidden');
        $body.find('.category-description-rw').removeClass('hidden');
    });
    // Reset category description
    $('.category-description-rw button[type=reset]').on('click', function(e) {
        var $this = $(this);
        var $body = $this.closest('.panel-body');
        $body.find('.category-description-ro').removeClass('hidden');
        $body.find('.category-description-rw').addClass('hidden');
    });
    // Any change disables all other controls because this isn't a proper ajax app
    $('input[type=text], textarea, select').not('#add-category-modal *').on('change input paste', function(e) {
        var $this = $(this);
        var $form = $this.closest('form');
        $('form').not($form).find('button, input[type=button], input[type=text], input[type=checkbox], textarea, select').prop('disabled', true);
        $('input[type=button], button').not($('form input[type=button], form button')).prop('disabled', true);
    });
    // Resetting form changes then re-enables all previously disabled controls
    $('input[type=reset], button[type=reset]').on('click', function(e) {
        $('form').find('button, input[type=button], input[type=text], input[type=checkbox], textarea, select').prop('disabled', false);
        $('input[type=button], button').not($('form input[type=button], form button')).prop('disabled', false);
    });
    // New filter form hide the reset button by default
    $('.new-filter input[type=text], .new-filter select').on('change', function(e) {
        $(this).closest('form').find('button[type=reset]').removeClass('hidden');
    });

    // every time user select/change 'negate', 'filter key' or 'filter value', generate a new fq
    $('.filter-row input[type=text], .filter-row select').on('change', generateFilterValue);
    $('.filter-row input[name=filterValue]').on('input', generateFilterValue);

    function generateFilterValue(e) {
        var group = $(e.target).closest('.filter-row');
        var exclude = $('.exclude', group).val();
        var filterKey = $('.filterKey', group).find(":selected").text();
        var filterVal = $('.filterValue', group).val();
        var newFqVal = (exclude == 'Include' ? '' : '-') + filterKey + (filterKey ? ':' : '') + filterVal;
        $('.filter', group).val(newFqVal);
    }

    // to reset current filter values
    $('.current-filter button[type=reset]').on('click', function(e) {
        e.preventDefault();
        var filtergroup = $(this).closest('.current-filter');
        // reset description controller's size and content
        $('.filterDescription', filtergroup).attr("style", "").val($('.filterDescription', filtergroup).data('orig'));
        // reset negate, filter key, filter value
        setFQGroup(filtergroup);
        // reset calculated fq
        $('.filter', filtergroup).val($('.filter', filtergroup).data('orig'));
    });

    $('.new-filter button[type=reset]').on('click', function(e) {
        $(this).addClass('hidden');
        $(this).closest('form').find('.filterDescription').attr("style", "");
    });

    $('input[type=checkbox][name=enabled]').on('change', function(e) {
        $(this).closest('form').submit();
    });

    $('.btn-load-filter-desc').on('click', function(e) {
        var $this = $(this);
        var $form = $this.closest('form');
        var $desc = $form.find('*[name=description]');
        var field = $form.find('*[name=filterKey]').val();
        var include = $form.find('select.exclude').val();
        var value = $form.find('*[name=filterValue]').val();
        if (!$desc.val() || confirm("Do you want to overwrite the current description?")) {
            $.get("${g.createLink(controller: 'adminDataQuality', action: 'fieldDescription')}", {
                    field: field,
                    include: include,
                    value: value
            }).done(function(data) {
                $desc.val(data);
                $desc.trigger("input");
            }).fail(function( jqXHR, textStatus, error ) {
                if (jqXHR.status === 404) {
                    alert( "No description found" );
                } else {
                    alert( "An error occured");
                }
            });
        }
    });
</asset:script>
</html>
