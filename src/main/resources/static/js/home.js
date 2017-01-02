$( document ).ready( function() {


	$( window ).on( 'hashchange', checkHash );
	checkHash();

	function checkHash() {

		switch ( window.location.hash.substring( 1 ) ) {
			case "applogin":
				showLogin( "app" );
				break;
			case "platformlogin":
				showLogin( "platform" );
				break;
			case "adminlogin":
				showLogin( "admin" );
				break;
			default:
				hideLogin();
				break;
		}
	}

	function showLogin( url ) {

		$.ajax( {
			url: url + "/cpanel",
			success: function( data ) {

				console.log( data );

				var dataDOM = $( $.trim( data ) );
				var login = dataDOM.find( "form.login" );

				if ( login.length > 0 ) {

					$( ".overlay .popup" ).html( login );
					$( ".overlay" ).css( { 'display': 'block' } );

					$( "form.login" ).submit( function( e ) {

						$.ajax( {
							type: "POST",
							url: url + "/login",
							data: $( "form.login" ).serialize(),
							success: function( data ) {

								window.location.href = url + "/cpanel";
							}
						} );

						e.preventDefault();
					} );

				} else {

					window.location.href = url + "/cpanel";
				}

			},
			error: function( data ) {

				console.log( data.responseText );

				$( ".overlay" ).html( $( $.trim( data.responseText ) ) );
				$( ".overlay" ).css( { 'display': 'block' } );
			}
		} );

	}

	function hideLogin() {

		$( ".overlay" ).css( { 'display': 'none' } );
	}


	$( document ).keyup( function( e ) {
		if ( $( ".overlay" ).css( 'display' ) != 'none' && e.keyCode == 27 ) { // escape pressed while login form open

			window.location.hash = "";
		}
	} );

	$( ".overlay.wrapper" ).click( function( e ) {

		if ( $( ".overlay" ).css( 'display' ) != 'none' && e.target == this ) { // background clicked while login form open

			window.location.hash = "";
		}
	} );


} );