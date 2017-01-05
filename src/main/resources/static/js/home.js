$( document ).ready( function() {


	$( window ).on( 'hashchange', checkHash );
	checkHash();

	function checkHash() {

		switch ( window.location.hash.substring( 1 ) ) {
			case "applogin":
				getPopup( "app/cpanel", "app" );
				break;
			case "platformlogin":
				getPopup( "platform/cpanel", "platform" );
				break;
			case "adminlogin":
				getPopup( "admin/cpanel", "admin" );
				break;
			case "appregister":
				getPopup( "register/app", "app" );
				break;
			case "platformregister":
				getPopup( "register/platform", "platform" );
				break;
			default:
				hidePopup();
				break;
		}
	}

	function getPopup( url, role ) {

		$.ajax( {
			url: url,
			success: function( data ) {

				var popup = $( "<div id='wrapper'>" + $.trim( data ) + "</div>" ).find( ".popup" );

				if ( popup.length > 0 ) {

					showPopup( popup, url, role );

				} else {

					window.location.href = url;
				}
			},
			error: function( data ) {

				$( ".overlay" ).html( $( $.trim( data.responseText ) ) );
				$( ".overlay" ).css( { 'display': 'block' } );
			}
		} );

	}

	function showPopup( popup, url, role ) {

		var login = popup.find( "form.login" );
		var register = popup.find( "form.register" );

		$( ".overlay .popup" ).html( popup.children( ":first" ) );
		$( ".overlay" ).css( { 'display': 'block' } );


		if ( login.length > 0 ) {

			handleLogin( url, role );

		} else {

			handleRegister( url, role );
		}
	}

	function hidePopup() {

		$( ".overlay" ).css( { 'display': 'none' } );
	}

	function handleLogin( url, role ) {

		$( "form.login" ).submit( function( e ) {

			$.ajax( {
				type: "POST",
				url: role + "/login",
				data: $( "form.login" ).serialize(),
				success: function( data ) {

					if ( $( data ).find( ".error" ).length == 0 ) {

						window.location.href = url;

					} else {

						showPopup( $( "<div id='wrapper'>" + $.trim( data ) + "</div>" ).find( ".popup" ), url, role );
					}
				}
			} );
			e.preventDefault();
		} );
	}

	function handleRegister( url, role ) {

		$( "form.register" ).submit( function( e ) {

			$.ajax( {
				type: "POST",
				url: url,
				data: $( "form.register" ).serialize(),
				success: function( data ) {

					if ( $( data ).find( ".error" ).length == 0 ) {

						showPopup( $( "<div id='wrapper'>" + $.trim( data ) + "</div>" ).find( ".popup" ), role + "/cpanel", role );

					} else {

						showPopup( $( "<div id='wrapper'>" + $.trim( data ) + "</div>" ).find( ".popup" ), url, role );
					}
				},
				error: function( data ) {

					$( ".overlay" ).html( $( $.trim( data.responseText ) ) );
					$( ".overlay" ).css( { 'display': 'block' } );
				}
			} );
			e.preventDefault();
		} );
	}


	$( document ).keyup( function( e ) {
		if ( $( ".overlay" ).css( 'display' ) != 'none' && e.keyCode == 27 ) { // escape pressed while popup open

			window.location.hash = "";
		}
	} );

	$( ".overlay.wrapper" ).click( function( e ) {

		if ( $( ".overlay" ).css( 'display' ) != 'none' && e.target == this ) { // background clicked while popup open

			window.location.hash = "";
		}
	} );


} );