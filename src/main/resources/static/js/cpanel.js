// Store useful html elements
var $initialPlatformModalContent = null;

var $platformPanelEntry = null;
var $infoModelPanelEntry = null;
var $federationPanelEntry = null;

var $platformRegistrationSuccessful = null;
var $platformSuccessfulDeletion = null;
var $platformRegistrationError = null;
var $deletePlatformError = null;

var $infoModelRegistrationSuccessful = null;
var $infoModelRegistrationError = null;
var $infoModelSuccessfulDeletion = null;
var $deleteInformationModelError = null;

var $federationRegistrationSuccessful = null;
var $federationRegistrationError = null;
var $federationSuccessfulDeletion = null;
var $deleteFederationError = null;

var $listOwnedPlatformsError = null;
var $listUserInfoModelError = null;
var $listFederationsError = null



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

function CreateFederationRequest(id, platform1Id, platform2Id) {
    this.id = id;
    this.platform1Id = platform1Id;
    this.platform2Id = platform2Id;
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
            $modal.removeClass('fade').modal('hide');

            buildPlatformPanels();
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
            $modal.removeClass('fade').modal('hide');

            // Refresh Information Models
            buildInfoModelsPanels();

            // Refresh Registration form
            buildPlatformRegistrationForm();
        },
        error : function(xhr) {
            var message = document.createElement('p');
            message.innerHTML = xhr.responseText;
            $('#information-models').prepend($deleteInformationModelError.clone().append(message).show());
            $modal.modal('hide');
        }
    });
});

$(document).on('click', '.del-federation-btn', function (e) {
    var $deleteButton = $(e.target);
    var $modal = $deleteButton.closest(".modal");
    var federationIdToDelete = $modal.attr('id').split('-').pop();


    $.ajax({
        url: "/user/cpanel/delete_federation",
        type: "POST",
        data: {federationIdToDelete : federationIdToDelete},
        success: function() {

            $('#federation_list').prepend($federationSuccessfulDeletion.clone().show());
            $modal.removeClass('fade').modal('hide');

            // Refresh Federation Panels
            buildFederationPanels();
        },
        error : function(xhr) {
            var message = document.createElement('p');
            message.innerHTML = JSON.parse(xhr.responseText).error;
            $('#federation_list').prepend($deleteFederationError.clone().append(message).show());
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

function storeNecessaryPlatformElements() {
    if ($platformPanelEntry === null) {
        // Deep clone
        $platformPanelEntry = $('#platform-entry').clone(true, true);
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

    if ($platformRegistrationSuccessful === null) {
        $platformRegistrationSuccessful = $('#platform-registration-successful').clone().removeAttr("id");
        $('#platform-registration-successful').remove();
    }

    if ($platformRegistrationError === null) {
        $platformRegistrationError = $('#platform-registration-error').clone().removeAttr("id");
        $('#platform-registration-error').remove();
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
            $('#platform-registration-info-model-id').find("option").remove();
            $platformPanelEntry.find('.selectpicker.update-info-model-select').find("option").remove();

            $.each(data, function (i, item) {
                $('#platform-registration-info-model-id').append($('<option>', {
                    value: item.id,
                    text : item.name
                }));

                $platformPanelEntry.find('.selectpicker.update-info-model-select').append($('<option>', {
                    value: item.id,
                    text : item.name
                }));
            });

            $('#platform-registration-info-model-id').selectpicker('refresh');

            buildPlatformPanels();
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


    $.ajax({
        url: "/user/cpanel/list_user_platforms",
        type: "POST",
        dataType: "json",
        contentType: "application/json",
        success: function(data, textStatus, jqXHR) {

            deletePlatformPanels();

            for (var i = 0; i < data.availablePlatforms.length; i++) {
                $platformTab.append(platformPanel(data.availablePlatforms[i]));
            }

            // Refresh the selectpickers
            $platformTab.find(".selectpicker.update-info-model-select").selectpicker("refresh");
            $platformTab.find(".selectpicker.update-info-model-select").nextAll("option").remove();
            $platformTab.find(".selectpicker.update-isEnabler").selectpicker("refresh");
            $platformTab.find(".selectpicker.update-isEnabler").nextAll("option").remove();


            if (jqXHR.status === 206) {
                var message = document.createElement('p');
                message.innerHTML = data.message;
                $('#platform-details').prepend($listOwnedPlatformsError.clone().append(message).show());
            }
        },
        error : function(xhr) {
            var message = document.createElement('p');
            message.innerHTML = JSON.parse(xhr.responseText).message;
            $('#platform-details').prepend($listOwnedPlatformsError.clone().append(message).show());
        }
    });
}

function platformPanel(ownedPlatform) {

    // Deep clone
    var $platform = $platformPanelEntry.clone(true);

    // Configuration of the platform panel
    var deleteplatformlId = "del-platform-modal-" + ownedPlatform.id;
    $platform.find('.panel-title').text(ownedPlatform.name);
    $platform.find('.btn-warning-delete').attr("data-target", "#" + deleteplatformlId);
    $platform.find('#platform-del-modal').attr("id", deleteplatformlId);
    $platform.find('.modal-title').find('strong').text(ownedPlatform.name);

    // Setting the platform details
    $platform.find('.platform-update-id').val(ownedPlatform.id);
    $platform.find('.platform-update-name').val(ownedPlatform.name);
    $platform.find('.platform-update-description').val(ownedPlatform.description);
    $platform.find('.selectpicker.update-isEnabler').val(ownedPlatform.isEnabler.toString());


    for (var iter = 0; iter < ownedPlatform.labels.length; iter++)
        $platform.find(".platform-update-labels").eq(iter).val(ownedPlatform.labels[iter].label);

    for (var iter = 0; iter < ownedPlatform.comments.length; iter++)
        $platform.find(".platform-update-comments").eq(iter).val(ownedPlatform.comments[iter].comment);

    for (var iter = 0; iter < ownedPlatform.interworkingServices.length; iter++) {
        $platform.find(".platform-update-interworking-service-url").eq(iter).val(ownedPlatform.interworkingServices[iter].url);
        $platform.find(".selectpicker.update-info-model-select").eq(iter).val(ownedPlatform.interworkingServices[iter].informationModelId);
    }

    $platform.show();
    return $platform;
}

function deletePlatformPanels() {
    $(".platform-panel-entry").remove();

}

function storeNecessaryInfoModelElements() {

    if ($infoModelPanelEntry === null) {
        $infoModelPanelEntry = $('#info-model-entry').clone(true, true);
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

    if ($infoModelRegistrationSuccessful === null) {
        $infoModelRegistrationSuccessful = $('#info-model-registration-successful').clone().removeAttr("id");
        $('#info-model-registration-successful').remove();
    }

    if ($infoModelRegistrationError === null) {
        $infoModelRegistrationError = $('#info-model-registration-error').clone().removeAttr("id");
        $('#info-model-registration-error').remove();
    }
}

// Construct information panels
function buildInfoModelsPanels() {
    var $infoModelTab = $('#information-models');

    $.ajax({
        url: "/user/cpanel/list_user_info_models",
        type: "POST",
        dataType: "json",
        contentType: "application/json",
        success: function(data) {
            deleteInfoModelsPanels();

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
    // Deep clone
    var $infoModelPanel = $infoModelPanelEntry.clone(true, true);

    // Configuration of the information model panel
    var deleteInfoModalId = "del-info-model-modal-" + infoModel.id;
    $infoModelPanel.find('.panel-title').text(infoModel.name);
    $infoModelPanel.find('.btn-warning-delete').attr("data-target", "#" + deleteInfoModalId);
    $infoModelPanel.find('#info-model-del-modal').attr("id", deleteInfoModalId);
    $infoModelPanel.find('.text-danger').find('strong').text(infoModel.name);

    // Setting the information model details
    $infoModelPanel.find('.update-info-model-name').val(infoModel.name);
    $infoModelPanel.find('.update-info-model-id').val(infoModel.id);
    $infoModelPanel.find('.update-info-model-uri').val(infoModel.uri);
    $infoModelPanel.find('.update-info-model-rdfformat').val(infoModel.rdfFormat);

    $infoModelPanel.show();
    return $infoModelPanel;
}

function deleteInfoModelsPanels() {
    $(".info-model-panel-entry").remove();
}

function storeNecessaryFederationElements() {

    if ($federationPanelEntry === null) {
        $federationPanelEntry = $('#federation-entry').clone(true, true);
        $('#federation-entry').remove();
    }

    if ($federationRegistrationSuccessful === null) {
        $federationRegistrationSuccessful = $('#federation-registration-successful').clone().removeAttr("id");
        $('#federation-registration-successful').remove();
    }

    if ($federationRegistrationError === null) {
        $federationRegistrationError = $('#federation-registration-error').clone().removeAttr("id");
        $('#federation-registration-error').remove();
    }

    if ($listFederationsError === null) {
        $listFederationsError = $('#list-federations-error').clone().removeAttr("id");
        $('#list-federations-error').remove();
    }

    if ($federationSuccessfulDeletion === null) {
        $federationSuccessfulDeletion = $('#leave-federation-successful').clone().removeAttr("id");
        $('#leave-federation-successful').remove();
    }

    if ($deleteFederationError === null) {
        $deleteFederationError = $('#leave-federation-error').clone().removeAttr("id");
        $('#leave-federation-error').remove();
    }
}

// Construct federation panels
function buildFederationPanels() {
    var $federationListTab = $('#federation_list');

    $.ajax({
        url: "/user/cpanel/list_federations",
        type: "POST",
        dataType: "json",
        contentType: "application/json",
        success: function(data) {
            deleteFederationPanels();

            for (var key in data) {
                $federationListTab.append(federationPanel(data[key]));
            }
        },
        error : function(xhr) {
            var message = document.createElement('p');
            message.innerHTML = JSON.parse(xhr.responseText).error;
            $federationListTab.prepend($listFederationsError.clone().append(message).show());
        }
    });
}

function federationPanel(federation) {

    // Deep clone
    var $federationPanel = $federationPanelEntry.clone(true, true);

    // Configuration of the federation panel
    var deleteInfoModalId = "del-federation-modal-" + federation.federationId;
    $federationPanel.find('.panel-title').text(federation.federationId);
    $federationPanel.find('.btn-warning-delete').attr("data-target", "#" + deleteInfoModalId);
    $federationPanel.find('#del-federation-modal').attr("id", deleteInfoModalId);
    $federationPanel.find('.text-danger').find('strong').text(federation.federationId);

    // Setting the federation details
    $federationPanel.find('.federation-id').text(federation.federationId);

    for (var i = 0; i < federation.platformIds.length; i++) {
        var platform = document.createElement('li');
        platform.innerHTML = federation.platformIds[i];
        platform.className += "list-group-item";
        $federationPanel.find('.federated-platforms').append(platform);
    }

    $federationPanel.show();
    return $federationPanel;
}

function deleteFederationPanels() {
    $(".federation-panel-entry").remove();
}

// On ready function
$(document).ready(function () {

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
                $('#platform-registration-modal-body').find('.alert-danger').hide();
                $('#platform-details').prepend($platformRegistrationSuccessful.clone().show());

                // Resetting the the form
                $('#platform-registration-form')[0].reset();
                $('#platform-registration-form').find('select').selectpicker("render");

                // Resetting the validation
                $('#platform-registration-form').validator('destroy').validator();

                buildPlatformPanels();
            },
            error : function(xhr) {
                $('#platform-registration-modal-body').find('.alert-danger').hide();
                var message = JSON.parse(xhr.responseText);

                var platformRegistrationError = document.createElement('p');
                platformRegistrationError.innerHTML = message.platformRegistrationError;
                $('#platform-registration-modal-body').prepend($platformRegistrationError.clone().append(platformRegistrationError).show());

                $('#platform-registration-modal').animate({ scrollTop: 0 }, 400);


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


    $("#info-model-reg-btn").click(function (e) {

        e.preventDefault();

        $('.progress').show();
        $('.myprogress').css('width', '0');
        $('.msg').text('');

        // Get form
        var form = $('#info-model-registration-form')[0];

        var data = new FormData(form);

        data.append("CustomField", "This is some extra data, testing");

        $("#info-model-reg-btn").prop("disabled", true);
        $('.msg').text('Uploading in progress...');

        $.ajax({
            type: "POST",
            enctype: 'multipart/form-data',
            url: "/user/cpanel/register_information_model",
            data: data,
            //http://api.jquery.com/jQuery.ajax/
            //https://developer.mozilla.org/en-US/docs/Web/API/FormData/Using_FormData_Objects
            processData: false, //prevent jQuery from automatically transforming the data into a query string
            contentType: false,
            cache: false,
            timeout: 600000,
            xhr: function () {
                var xhr = new window.XMLHttpRequest();
                xhr.upload.addEventListener("progress", function (evt) {
                    if (evt.lengthComputable) {
                        var percentComplete = evt.loaded / evt.total;
                        percentComplete = parseInt(percentComplete * 100);
                        $('.myprogress').text(percentComplete + '%');
                        $('.myprogress').css('width', percentComplete + '%');
                    }
                }, false);
                return xhr;
            },
            success: function (data) {
                $("#info-model-reg-btn").prop("disabled", false);
                $('.progress').show();
                $('.myprogress').css('width', '0%').text('');
                $('.msg').text('');

                $('#info-model-reg-modal').modal('hide');
                $('#info-model-registration-modal-body').find('.alert-danger').hide();
                $('#information-models').prepend($infoModelRegistrationSuccessful.clone().show());

                // Resetting the the form
                $('#info-model-name').val("");
                $('#info-model-uri').val("");
                $('#info-model-rdf').val("");

                // Resetting the validation
                $('#info-model-registration-form').validator('destroy').validator();

                // Refresh Information Models
                buildInfoModelsPanels();

                // Refresh Registration form
                buildPlatformRegistrationForm();

            },
            error: function (xhr) {
                $("#info-model-reg-btn").prop("disabled", false);
                $('.myprogress').css('width', '0%').text('');
                $('.msg').text('');

                $('#info-model-registration-modal-body').find('.alert-danger').hide();
                var message = JSON.parse(xhr.responseText);

                var infoModelRegistrationError = document.createElement('p');
                infoModelRegistrationError.innerHTML = message.error;
                $('#info-model-registration-modal-body').prepend($infoModelRegistrationError.clone().append(infoModelRegistrationError).show());

                $('#info-model-reg-modal').animate({ scrollTop: 0 }, 400);

                if (typeof message.info_model_reg_error_name !== 'undefined')
                    $('#info-model-reg-error-name').html(message.info_model_reg_error_name).show();

                if (typeof message.info_model_reg_error_uri !== 'undefined')
                    $('#info-model-reg-error-uri').html(message.info_model_reg_error_uri).show();

                if (typeof message.info_model_reg_error_rdf !== 'undefined')
                    $('#info-model-reg-error-rdf').html(message.info_model_reg_error_rdf).show();
            }
        });
    });

    $('#federation-reg-btn').click(function (e) {
        e.preventDefault();


        var federationId = $('#federation-registration-id').val();
        var platform1 = $('#federated-platform-id-1').val();
        var platform2 = $('#federated-platform-id-2').val();

        var newFederation = new CreateFederationRequest(federationId, platform1, platform2);

        $.ajax({
            type : "POST",
            url : "/user/cpanel/create_federation",
            dataType: "json",
            contentType: "application/json",
            data : JSON.stringify(newFederation),
            success : function (data) {

                $('#federation-reg-modal').modal('hide');
                $('#federation-registration-modal-body').find('.alert-danger').hide();
                $('#federation_list').prepend($federationRegistrationSuccessful.clone().show());

                // Resetting the the form
                $('#federation-registration-form')[0].reset();

                // Resetting the validation
                $('#federation-registration-form').validator('destroy').validator();

                buildFederationPanels();
            },
            error : function(xhr) {
                $('#federation-registration-modal-body').find('.alert-danger').hide();
                var message = JSON.parse(xhr.responseText);

                var federationRegistrationError = document.createElement('p');
                federationRegistrationError.innerHTML = message.error;
                $('#federation-registration-modal-body').prepend($federationRegistrationError.clone().append(federationRegistrationError).show());

                if (typeof message.federation_reg_error_id !== 'undefined')
                    $('#federation-reg-error-id').html(message.federation_reg_error_id).show();

                if (typeof message.federation_reg_error_platform1Id !== 'undefined')
                    $('#federation-reg-error-platform1-id').html(message.federation_reg_error_platform1Id).show();

                if (typeof message.federation_reg_error_platform2Id !== 'undefined')
                    $('#federation-reg-error-platform2-id').html(message.federation_reg_error_platform2Id).show();


            }
        });

    });

});