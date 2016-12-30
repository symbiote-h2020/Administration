$( document ).ready( function() {


	$( window ).on( 'hashchange', function() {

		checkHash();
	} );
	checkHash();

	function checkHash() {

		switch ( window.location.hash.substring( 1 ) ) {
			case "adminlogin":
				showLogin( "Administrator Login" );
				break;
			case "platformlogin":
				showLogin( "Platform Provider Login" );
				break;
			case "userlogin":
				showLogin( "User/App Developer Login" );
				break;
			default:
				hideLogin();
				break;
		}
	}

	function showLogin( title ) {

		$( ".overlay .title" ).text( title );
		$( ".overlay" ).css( { 'display': 'block' } );
	}

	function hideLogin() {

		$( ".overlay" ).css( { 'display': 'none' } );
	}

	$( document ).keyup( function( e ) {
		if ( $( ".overlay" ).css( 'display') != 'none' && e.keyCode == 27 ) { // escape pressed while login form open
			
			window.location.hash = "";
		}
	} );

	$( ".overlay.wrapper" ).click(function(e){

		if ( $( ".overlay" ).css( 'display') != 'none' && e.target == this ) { // background clicked while login form open
			
			window.location.hash = "";
		}
	});


} );