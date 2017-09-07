// Store useful html elements
var $platformPanelEntry = null;
var $infoModelPanelEntry = null;
var $platformSuccessfulDeletion = null;
var $listOwnedPlatformsError = null;
var $deletePlatformError = null;
var $infoModelSuccessfulDeletion = null;
var $deleteInformationModelError = null;
var $listUserInfoModelError = null;
var $initialPlatformModalContent = null;

// Store csrf token
var csrfToken = null;
var csrfHeader = null;

// Class definitions
function InterworkingService(url, informationModelId) {
    this.url = url;
    this.informationModelId = informationModelId;
}

function Platform(id, name, description, labels, comments, interworkingServices, isEnabler) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.labels = labels;
    this.comments = comments;
    this.interworkingServices = interworkingServices;
    this.isEnabler = isEnabler;
}

function InformationModel(id, uri, name, owner, rdf, rdfFormat) {
    this.id = id;
    this.uri = uri;
    this.name = name;
    this.owner = owner;
    this.rdf = rdf;
    this.rdfFormat = rdfFormat;
}

// Event handlers
$(document).on('click', '.panel div.clickable', function () {
    var $this = $(this);
    if (!$this.hasClass('panel-collapsed')) {
        $this.parents('.panel').find('.panel-body').slideToggle();
        $this.addClass('panel-collapsed');
        $this.find('i').removeClass('glyphicon-plus').addClass('glyphicon-minus');
    } else {
        $this.parents('.panel').find('.panel-body').slideToggle();
        $this.removeClass('panel-collapsed');
        $this.find('i').removeClass('glyphicon-minus').addClass('glyphicon-plus');
    }
});

$(document).on('click', '.del-platform-btn', function (e) {
    var $deleteButton = $(e.target);
    var $modal = $deleteButton.closest(".modal");
    var platformIdToDelete = $modal.attr('id').split('-').pop();


    $.ajax({
        url: "/user/cpanel/delete_platform",
        type: "POST",
        data: {platformIdToDelete : platformIdToDelete},
        success: function(data) {
            $('#platform-details').prepend($platformSuccessfulDeletion.clone().show());
            $modal.modal('hide');

        },
        error : function(xhr) {
            var message = document.createElement('p');
            message.innerHTML = xhr.responseText;
            $('#platform-details').prepend($deletePlatformError.clone().append(message).show());
            $modal.modal('hide');
        }
    });
});

$(document).on('click', '.del-info-model-btn', function (e) {
    var $deleteButton = $(e.target);
    var $modal = $deleteButton.closest(".modal");
    var infoModelIdToDelete = $modal.attr('id').split('-').pop();


    $.ajax({
        url: "/user/cpanel/delete_information_model",
        type: "POST",
        data: {infoModelIdToDelete : infoModelIdToDelete},
        success: function(data) {
            $('#information-models').prepend($infoModelSuccessfulDeletion.clone().show());
            $modal.modal('hide');

        },
        error : function(xhr) {
            var message = document.createElement('p');
            message.innerHTML = xhr.responseText;
            $('#information-models').prepend($deleteInformationModelError.clone().append(message).show());
            $modal.modal('hide');
        }
    });
});

// Setting up ajax queries
function setupAjax() {
    if (csrfToken === null || csrfHeader === null) {
        csrfToken = $("meta[name='_csrf']").attr("content");
        csrfHeader = $("meta[name='_csrf_header']").attr("content");

        $.ajaxSetup({
            beforeSend: function(xhr) {
                xhr.setRequestHeader(csrfHeader, csrfToken);
            }
        });
    }
}

function buildPlatformRegistrationForm() {
    var $form = $('#platform-registration-form');

    $.ajax({
        url: "/user/cpanel/list_all_info_models",
        type: "POST",
        dataType: "json",
        contentType: "application/json",
        success: function(data) {
            $.each(data, function (i, item) {
                $('#platform-registration-info-model-id').append($('<option>', {
                    value: item.id,
                    text : item.name
                }));
            });

            $('#platform-registration-info-model-id').selectpicker('refresh');

        },
        error : function(xhr) {
            var message = document.createElement('p');
            message.innerHTML = xhr.responseText;
            $('#list-all-info-model-error').append(message).show();
        }
    });
}

// Construct Platform Panels
function buildPlatformPanels() {
    var $platformTab = $('#platform-details');

    if ($platformPanelEntry === null) {
        $platformPanelEntry = $('#platform-entry').clone();
        $('#platform-entry').remove();
    }

    if ($platformSuccessfulDeletion === null) {
        $platformSuccessfulDeletion = $('#platform-successful-deletion').clone().removeAttr("id");
        $('#platform-successful-deletion').remove();
    }

    if ($deletePlatformError === null) {
        $deletePlatformError = $('#delete-platform-error').clone().removeAttr("id");
        $('#delete-platform-error').remove();
    }

    if ($listOwnedPlatformsError === null) {
        $listOwnedPlatformsError = $('#list-owned-platforms-error').clone().removeAttr("id");
        $('#list-owned-platforms-error').remove();
    }
    $.ajax({
        url: "/user/cpanel/list_user_platforms",
        type: "POST",
        dataType: "json",
        contentType: "application/json",
        success: function(data) {
            for (var i = 0; i < data.length; i++) {
                $platformTab.append(platformPanel(data[i]));
            }
        },
        error : function(xhr) {
            var message = document.createElement('p');
            message.innerHTML = xhr.responseText;
            $('#platform-details').prepend($listOwnedPlatformsError.clone().append(message).show());
        }
    });
}

function platformPanel(ownedPlatform) {
    var $platform = $platformPanelEntry.clone();

    var deleteplatformlId = "del-platform-modal-" + ownedPlatform.platformInstanceId;
    $platform.find('.panel-title').text(ownedPlatform.platformInstanceFriendlyName);
    $platform.find('.panel-body').text(ownedPlatform.platformInstanceId);
    $platform.find('.btn-warning-delete').attr("data-target", "#" + deleteplatformlId);
    $platform.find('#PLATFORM-DEL-MODAL').attr("id", deleteplatformlId);
    $platform.find('.modal-title').find('strong').text(ownedPlatform.platformInstanceFriendlyName);
    $platform.show();
    return $platform;
}

// Construct information panels
function buildInfoModelsPanels() {
    var $infoModelTab = $('#information-models');

    if (csrfToken === null || csrfHeader === null) {
        csrfToken = $("meta[name='_csrf']").attr("content");
        csrfHeader = $("meta[name='_csrf_header']").attr("content");

        $.ajaxSetup({
            beforeSend: function(xhr) {
                xhr.setRequestHeader(csrfHeader, csrfToken);
            }
        });
    }

    if ($infoModelPanelEntry === null) {
        $infoModelPanelEntry = $('#info-model-entry').clone();
        $('#info-model-entry').remove();
    }

    if ($infoModelSuccessfulDeletion === null) {
        $infoModelSuccessfulDeletion = $('#info-model-successful-deletion').clone().removeAttr("id");
        $('#info-model-successful-deletion').remove();
    }

    if ($deleteInformationModelError === null) {
        $deleteInformationModelError = $('#delete-information-model-error').clone().removeAttr("id");
        $('#delete-information-model-error').remove();
    }

    if ($listUserInfoModelError === null) {
        $listUserInfoModelError = $('#list-user-info-model-error').clone().removeAttr("id");
        $('#list-user-info-model-error').remove();
    }

    $.ajax({
        url: "/user/cpanel/list_user_info_models",
        type: "POST",
        dataType: "json",
        contentType: "application/json",
        success: function(data) {
            for (var i = 0; i < data.length; i++) {
                $infoModelTab.append(infoModelPanel(data[i]));
            }
        },
        error : function(xhr) {
            var message = document.createElement('p');
            message.innerHTML = xhr.responseText;
            $('#information-models').prepend($listUserInfoModelError.clone().append(message).show());
        }
    });
}

function infoModelPanel(infoModel) {
    var $infoModelPanel = $infoModelPanelEntry.clone();

    var deleteInfoModalId = "del-info-model-modal-" + infoModel.id;
    $infoModelPanel.find('.panel-title').text(infoModel.name);
    $infoModelPanel.find('.panel-body').text(infoModel.owner);
    $infoModelPanel.find('.btn-warning-delete').attr("data-target", "#" + deleteInfoModalId);
    $infoModelPanel.find('#INFO-MODEL-DEL-MODAL').attr("id", deleteInfoModalId);
    $infoModelPanel.find('.text-danger').find('strong').text(infoModel.name);
    $infoModelPanel.show();
    return $infoModelPanel;
}

function deleteInfoModelsPanel() {
    $(".panelEntry").remove();
}

function registerInfoModel() {
    var informationModel = new InformationModel(
        $('#infoModelId').val(),
        $('#infoModelUri').val(),
        $('#infoModelName').val(),
        $('#infoModelOwner').val(),
        $('#infoModelRdf').val(),
        $('#infoModelRdfFormat').val());

    $.ajax({
        url: "/user/cpanel/register_information_model",
        type: "POST",
        dataType: "json",
        contentType: "application/json",
        data: JSON.stringify(informationModel),
        success: function(data) {
            $('#infoModelRegModal').modal('hide');
            $('#infoModelId').val('');
            $('#infoModelUri').val('');
            $('#infoModelName').val('');
            $('#infoModelOwner').val('');
            $('#infoModelRdf').val('');
            $('#infoModelRdfFormat').val('');

        },
        error : function() {
            alert("There was an error!");
        }
    });
}

// On ready function
$(document).ready(function () {
    if (document.getElementById("platformRegistrationError") !== null) {
        $('#platform-registration-modal').modal('show');
    }

    $('#registration-platform-btn').click(function (e) {
        e.preventDefault();


        var platformId = $('#platform-registration-id').val();
        var platformName = $('#platform-registration-name').val();
        var platformDescription = $('#platform-registration-description').val();
        var labels = $.map($('.registration-labels'), function (elem) { return elem.value; });
        var comments = $.map($('.registration-comments'), function (elem) { return elem.value; });
        var isEnabler = $('#isEnabler').val();

        var interworkingServicesUrls = $.map($('.interworking-service-url'), function (elem) { return elem.value; });
        var interworkingServicesModels = $.map($('.info-model-select'), function (elem) { return elem.value });
        var interworkingServices = [];
        for (var i = 0; i < interworkingServicesUrls.length; i++) {
            interworkingServices.push(new InterworkingService(interworkingServicesUrls[i], interworkingServicesModels[i]));
        }

        var newPlatform = new Platform(platformId, platformName, platformDescription, labels, comments, interworkingServices, isEnabler);

        $.ajax({
            type : "POST",
            url : "/user/cpanel/register_platform",
            dataType: "json",
            contentType: "application/json",
            data : JSON.stringify(newPlatform),
            success : function (data) {

                $('#platform-registration-modal').modal('hide');
                $('#platform-details').prepend($('#platform-registration-successful').clone().removeAttr("id").show());

                // Resetting the the form
                $('#platform-registration-form')[0].reset();
                $('#platform-registration-form').find('select').selectpicker("render");

                // Resetting the validation
                $('#platform-registration-form').validator('destroy').validator();
            },
            error : function(xhr) {
                $('#platform-registration-row').find('.alert-danger').hide();
                var message = JSON.parse(xhr.responseText);

                var platformRegistrationError = document.createElement('p');
                platformRegistrationError.innerHTML = message.platformRegistrationError;
                $('#platform-registration-modal-body').prepend($('#platform-registration-error').clone().append(platformRegistrationError).removeAttr("id").show());

                if (typeof message.pl_reg_error_id !== 'undefined')
                    $('#pl-reg-error-id').html(message.pl_reg_error_id).show();

                if (typeof message.pl_reg_error_name !== 'undefined')
                    $('#pl-reg-error-name').html(message.pl_reg_error_name).show();

                if (typeof message.pl_reg_error_description !== 'undefined')
                    $('#pl-reg-error-description').html(message.pl_reg_error_description).show();

                if (typeof message.pl_reg_error_labels_label !== 'undefined') {
                    for(var i = 0; i < message.pl_reg_error_labels_label.length; i++)
                        if(message.pl_reg_error_labels_label[i] != null)
                            $('.pl-reg-error-label').eq(i).html(message.pl_reg_error_labels_label[i]).show();
                }

                if (typeof message.pl_reg_error_comments_comment !== 'undefined') {
                    for(var i = 0; i < message.pl_reg_error_comments_comment.length; i++)
                        if(message.pl_reg_error_comments_comment[i] != null)
                            $('.pl-reg-error-comment').eq(i).html(message.pl_reg_error_comments_comment[i]).show();
                }

                if (typeof message.pl_reg_error_interworkingServices_url !== 'undefined') {
                    for(var i = 0; i < message.pl_reg_error_interworkingServices_url.length; i++)
                        if(message.pl_reg_error_interworkingServices_url[i] != null)
                            $('.pl-reg-error-interworkingServices-url').eq(i).html(message.pl_reg_error_interworkingServices_url[i]).show();
                }

                if (typeof message.pl_reg_error_interworkingServices_informationModelId !== 'undefined') {
                    for(var i = 0; i < message.pl_reg_error_interworkingServices_url.length; i++)
                        if(message.pl_reg_error_interworkingServices_informationModelId[i] != null)
                            $('.pl-reg-error-interworkingServices-informationModelId').eq(i).html(message.pl_reg_error_interworkingServices_informationModelId[i]).show();
                }

                if (typeof message.pl_reg_error_isEnabler !== 'undefined')
                    $('#pl-reg-error-isEnabler').html(message.pl_reg_error_isEnabler).show();

            }
        });

    });

    $initialPlatformModalContent = $('#platform-registration-modal').clone(true, true);

});