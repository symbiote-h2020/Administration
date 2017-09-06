var $platformPanelEntry = null;
var $infoModelPanelEntry = null;
var $platformSuccessfulDeletion = null;
var $listOwnedPlatformsError = null;
var $deletePlatformError = null;
var $infoModelSuccessfulDeletion = null;
var $deleteInformationModelError = null;
var $listUserInfoModelError = null;
var csrfToken = null;
var csrfHeader = null;


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
            $('#platform-details').find('h3').eq(0).after($platformSuccessfulDeletion.clone().show());
            $modal.modal('hide');

        },
        error : function(xhr) {
            var message = document.createElement('p');
            message.innerHTML = xhr.responseText;
            $('#platform-details').find('h3').eq(0).after($deletePlatformError.clone().append(message).show());
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
            $('#information-models').find('h3').eq(0).after($infoModelSuccessfulDeletion.clone().show());
            $modal.modal('hide');

        },
        error : function(xhr) {
            var message = document.createElement('p');
            message.innerHTML = xhr.responseText;
            $('#information-models').find('h3').eq(0).after($deleteInformationModelError.clone().append(message).show());
            $modal.modal('hide');
        }
    });
});

$(document).ready(function () {
    if (document.getElementById("platformRegistrationError") !== null) {
        $('#platformRegistrationModal').modal('show');
    }
});

function buildPlatformPanels() {
    var $platformTab = $('#platform-details');

    if (csrfToken === null || csrfHeader === null) {
        csrfToken = $("meta[name='_csrf']").attr("content");
        csrfHeader = $("meta[name='_csrf_header']").attr("content");

        $.ajaxSetup({
            beforeSend: function(xhr) {
                xhr.setRequestHeader(csrfHeader, csrfToken);
            }
        });
    }

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
            $('#platform-details').find('h3').eq(0).after($listOwnedPlatformsError.clone().append(message).show());
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
            $('#information-models').find('h3').eq(0).after($listUserInfoModelError.clone().append(message).show());
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