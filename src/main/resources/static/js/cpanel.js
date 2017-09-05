function InformationModel(id, uri, name, owner, rdf, rdfFormat) {
    this.id = id;
    this.uri = uri;
    this.name = name;
    this.owner = owner;
    this.rdf = rdf;
    this.rdfFormat = rdfFormat;
}

$(document).on('click', '.panel div.clickable', function (e) {
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
    if (document.getElementById("platformRegistrationError") != null) {
        $('#platformRegistrationModal').modal('show');
    }

    // $('#platformRegFrom').formValidation();
});

function toggleElement(elementId) {
    var x = document.getElementById(elementId);
    if (x.style.display === 'none') {
        x.style.display = 'block';
    } else {
        x.style.display = 'none';
    }
}

function toggleElement(elementId1, elementId2) {
    var x = document.getElementById(elementId1);
    var y = document.getElementById(elementId2);

    if (x.style.display === 'none') {
        x.style.display = 'block';
    } else {
        x.style.display = 'none';
    }

    if (y.style.display === 'none') {
        y.style.display = 'block';
    } else {
        y.style.display = 'none';
    }
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


function buildInfoModelsPanel() {
    var infoModelTab = document.getElementById("information_models");
    var token = $("meta[name='_csrf']").attr("content");
    var header = $("meta[name='_csrf_header']").attr("content");

    $.ajaxSetup({
        beforeSend: function(xhr) {
            xhr.setRequestHeader(header, token);
        }
    });

    var response;
    $.ajax({
        url: "/user/cpanel/list_user_info_models",
        type: "POST",
        dataType: "json",
        contentType: "application/json",
        success: function(data) {
            for (var i = 0; i < data.length; i++) {
                infoModelTab.appendChild(infoModelPanel(data[i]));
            }
        },
        error : function(xhr) {
            var infoModelListError = document.getElementsByClassName("information-model-list-error");
            var message = document.createElement('p');
            message.innerHTML = xhr.responseText;
            $(".information-model-list-error").append(message).show();
        }
    });


}

function infoModelPanel(infoModel) {
    var infoModelPanel = document.createElement('div');
    infoModelPanel.classList.add("panel", "panel-primary", "panelEntry");

    infoModelPanel.innerHTML = '' +
        '                        <div class="panel-heading clickable">\n' +
        '                            <h3 class="panel-title">\n' +
                                     infoModel.name +
        '                            </h3>\n' +
        '                            <span class="pull-right"><i class="glyphicon glyphicon-plus"></i></span>\n' +
        '                        </div>\n' +
        '                        <div class="panel-body" style="display: none;">\n' +
                                     infoModel.owner +
        '                        </div>\n' +
        '                        <div class="panel-footer platform-info-footer"></div>';

    return infoModelPanel;
}

function deleteInfoModelsPanel() {
    $(".panelEntry").remove();
}