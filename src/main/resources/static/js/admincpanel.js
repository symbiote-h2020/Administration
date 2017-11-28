// Store useful html elements
var $federationPanelEntry = null;
var $listFederationsError = null;

var $platformResourcesSuccessfulDeletion = null;
var $deletePlatformResourcesError = null;

var $infoModelSuccessfulDeletion = null;
var $deleteInformationModelError = null;

var $federationRegistrationSuccessful = null;
var $federationRegistrationError = null;
var $federationSuccessfulDeletion = null;
var $deleteFederationError = null;


// Store csrf token
var csrfToken = null;
var csrfHeader = null;

// Class definitions

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


$(document).on('click', '.del-federation-btn', function (e) {
    var $deleteButton = $(e.target);
    var $modal = $deleteButton.closest(".modal");
    var federationIdToDelete = $modal.attr('id').split('del-federation-modal-').pop();


    $.ajax({
        url: "/administration/admin/cpanel/delete_federation",
        type: "POST",
        data: {federationIdToDelete : federationIdToDelete},
        success: function() {

            $('#federation_list').prepend($federationSuccessfulDeletion.clone().show());
            $modal.removeClass('fade').modal('hide');

            // Refresh Federation Panels
            buildFederationPanels();
        },
        error : function(xhr) {
            if (xhr.status === 405) {
                window.location.href = "/administration/admin/login";
            } else {
                var message = document.createElement('p');
                message.innerHTML = JSON.parse(xhr.responseText).error;
                $('#federation_list').prepend($deleteFederationError.clone().append(message).show());
                $modal.modal('hide');
            }
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


function storeNecessaryElements() {

    if ($platformResourcesSuccessfulDeletion === null) {
        $platformResourcesSuccessfulDeletion = $('#platform-resources-successful-deletion').clone().removeAttr("id");
        $('#platform-resources-successful-deletion').remove();
    }

    if ($deletePlatformResourcesError === null) {
        $deletePlatformResourcesError = $('#delete-platform-resources-error').clone().removeAttr("id");
        $('#delete-platform-resources-error').remove();
    }
    
    if ($infoModelSuccessfulDeletion === null) {
        $infoModelSuccessfulDeletion = $('#info-model-successful-deletion').clone().removeAttr("id");
        $('#info-model-successful-deletion').remove();
    }

    if ($deleteInformationModelError === null) {
        $deleteInformationModelError = $('#delete-information-model-error').clone().removeAttr("id");
        $('#delete-information-model-error').remove();
    }
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
        url: "/administration/admin/cpanel/list_federations",
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
            if (xhr.status === 405) {
                window.location.href = "/administration/admin/login";
            }
            else {
                var message = document.createElement('p');
                message.innerHTML = JSON.parse(xhr.responseText).error;
                $federationListTab.prepend($listFederationsError.clone().append(message).show());
            }
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

    $('#admin-delete-platform-resources-btn').click(function (e) {

        var platformId = $('#admin-delete-platform-resources-input').val();

        $.ajax({
            url: "/administration/admin/cpanel/delete_platform_resources",
            type: "POST",
            data: {platformId : platformId},
            success: function() {
                $('#admin-delete-platform-resources').prepend($platformResourcesSuccessfulDeletion.clone().show());
            },
            error : function(xhr) {
                if (xhr.status === 405) {
                    window.location.href = "/administration/admin/login";
                } else {
                    var message = document.createElement('p');
                    message.innerHTML = xhr.responseText;
                    $('#admin-delete-platform-resources').prepend($deletePlatformResourcesError.clone().append(message).show());
                }
            }
        });
    });
    
    $('#admin-delete-info-model-btn').click(function (e) {

        var infoModelIdToDelete = $('#admin-delete-info-model-input').val();

        $.ajax({
            url: "/administration/admin/cpanel/delete_information_model",
            type: "POST",
            data: {infoModelIdToDelete : infoModelIdToDelete},
            success: function() {
                $('#admin-delete-info-model').prepend($infoModelSuccessfulDeletion.clone().show());
            },
            error : function(xhr) {
                if (xhr.status === 405) {
                    window.location.href = "/administration/admin/login";
                } else {
                    var message = document.createElement('p');
                    message.innerHTML = xhr.responseText;
                    $('#admin-delete-info-model').prepend($deleteInformationModelError.clone().append(message).show());
                }
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
            url : "/administration/admin/cpanel/create_federation",
            dataType: "json",
            contentType: "application/json",
            data : JSON.stringify(newFederation),
            success : function () {

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
                if (xhr.status === 405) {
                    window.location.href = "/administration/admin/login";
                } else {
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
            }
        });

    });
});