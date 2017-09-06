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

$(document).ready(function () {
    if (document.getElementById("platformRegistrationError") !== null) {
        $('#platformRegistrationModal').modal('show');
    }

    // $('#platformRegFrom').formValidation();
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
        $infoModelPanelEntry = $("#info-model-entry").clone();
        $("#info-model-entry").remove();
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

    var deleteInfoModalId = "info-model-modal-" + infoModel.id;
    $infoModelPanel.find(".panel-title").text(infoModel.name);
    $infoModelPanel.find(".panel-body").text(infoModel.owner);
    $infoModelPanel.find(".btn-warning-delete").attr("data-target", "#" + deleteInfoModalId);
    $infoModelPanel.find("#INFO-MODEL-MODAL").attr("id", deleteInfoModalId);
    $infoModelPanel.find(".text-danger").find("strong").text(infoModel.name);
    $infoModelPanel.show();

    return $infoModelPanel;
}

function deleteInfoModelsPanel() {
    $(".panelEntry").remove();
}

function registerInfoModel() {
    var informationModel = new InformationModel(
        document.getElementById("infoModelId").value,
        document.getElementById("infoModelUri").value,
        document.getElementById("infoModelName").value,
        document.getElementById("infoModelOwner").value,
        document.getElementById("infoModelRdf").value,
        document.getElementById("infoModelRdfFormat").value);

    $.ajax({
        url: "/user/cpanel/reg_info_model",
        type: "POST",
        dataType: "json",
        contentType: "application/json",
        data: JSON.stringify(informationModel),
        success: function(data) {
            alert(JSON.stringify(data));
            $('#infoModelModal').modal('hide');
            document.getElementById("infoModelId").value = '';
            document.getElementById("infoModelUri").value = '';
            document.getElementById("infoModelName").value = '';
            document.getElementById("infoModelOwner").value = '';
            document.getElementById("infoModelRdf").value = '';
            document.getElementById("infoModelRdfFormat").value = '';

        },
        error : function() {
            alert("There was an error!");
        }
    });
}