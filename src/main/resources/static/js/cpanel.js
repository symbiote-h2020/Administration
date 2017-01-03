$( document ).ready( function() {


	$(".action").click(function(){

		if( ! $(this).hasClass("active") ){

			$(".action.active").removeClass("active");
			$(".tab.active").removeClass("active");

			$(this).addClass("active");
			$( "#" + $(this).attr("id") + "_tab" ).addClass("active");
		}

	});


} );