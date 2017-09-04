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