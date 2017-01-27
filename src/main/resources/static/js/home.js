$( document ).ready( function() {

	// Show a login or register popup

	$( ".admin.button" ).click(function(e){

		getPopup( "admin/cpanel", "admin" );
	});

	$( ".login.button" ).click(function(e){

		getPopup( "user/cpanel", "user" );
	});
	$( ".register.button" ).click(function(e){

		getPopup( "register", "user" );
	});


	// Retrieve the popup html and show it

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

			handleRegister( url );
		}
	}

	function hidePopup() {

		$( ".overlay" ).css( { 'display': 'none' } );
	}


	// handle login/register form submission

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

	function handleRegister( url ) {

		$( "form.register" ).submit( function( e ) {

			$.ajax( {
				type: "POST",
				url: url,
				data: $( "form.register" ).serialize(),
				success: function( data ) {

					if ( $( data ).find( ".error" ).length == 0 ) {

						showPopup( $( "<div id='wrapper'>" + $.trim( data ) + "</div>" ).find( ".popup" ), "user/cpanel", "user" );

					} else {

						showPopup( $( "<div id='wrapper'>" + $.trim( data ) + "</div>" ).find( ".popup" ), url,  "user" );
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

	// Hide the popup

	$( document ).keyup( function( e ) {
		if ( $( ".overlay" ).css( 'display' ) != 'none' && e.keyCode == 27 ) { // escape pressed while popup open

			hidePopup();
		}
	} );

	$( ".overlay.wrapper" ).click( function( e ) {

		if ( $( ".overlay" ).css( 'display' ) != 'none' && e.target == this ) { // background clicked while popup open

			hidePopup();
		}
	} );

} );