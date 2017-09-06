var $infoModelPanelEntry = null;
var csrfToken = null;
var csrfHeader = null;

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

$(document).on('click', '.del-info-model-btn', function (e) {
    var $deleteButton = $(e.target);
    var $modal = $deleteButton.closest(".modal");
    var infoModelIdToDelete = $modal.attr('id').split('-').pop();


    $.ajax({
        url: "/user/cpanel/delete_information_model",
        type: "POST",
        data: {infoModelIdToDelete : infoModelIdToDelete},
        success: function(data) {
            $('#info-model-successful-deletion').after($('#info-model-successful-deletion').clone().removeAttr("id").show());
            $modal.modal('hide');

        },
        error : function(xhr) {
            var message = document.createElement('p');
            message.innerHTML = xhr.responseText;
            $("#delete-information-model-error").after($("#delete-information-model-error").clone().removeAttr("id").append(message).show());
            $modal.modal('hide');
        }
    });
});

$(document).ready(function () {
    if (document.getElementById("platformRegistrationError") !== null) {
        $('#platformRegistrationModal').modal('show');
    }
});

function buildInfoModelsPanel() {
    var $infoModelTab = $("#information_models");

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
            $(".information-model-list-error").append(message).show();
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