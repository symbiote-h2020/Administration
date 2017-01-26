$( document ).ready( function() {

	console.log("running");
	console.log(window.location.hash);

    if(!window.location.hash) {
        window.location = window.location + '#loaded';
        setTimeout(function(){
			window.location.reload(1);
		}, 5000);
    }
	


} );