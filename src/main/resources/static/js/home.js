$( document ).ready( function() {

	// Show a login or register popup

	$( ".admin.button" ).click(function(e){

		getPopup( "/admin/login" );
		e.preventDefault();
	});

	$( ".app .login.button" ).click(function(e){

		getPopup( "/user/login" );
		e.preventDefault();
	});
	$( ".platform .login.button" ).click(function(e){

		getPopup( "/user/login" );
		e.preventDefault();
	});

	$( ".app .register.button" ).click(function(e){

		getPopup( "/register/app" );
		e.preventDefault();
	});
	$( ".platform .register.button" ).click(function(e){

		getPopup("/register");
		e.preventDefault();
	});


	// Retrieve the popup html and show it

	function getPopup( url ) {

		$.ajax( {
			url: url,
			success: function( data ) {

				showResponse(data, url);

				var popup = $( "<div id='wrapper'>" + $.trim( data ) + "</div>" ).find( ".popup" );

				
			},
			error: function( data ) {

				$( ".overlay" ).html( $( $.trim( data ) ) );
				$( ".overlay" ).css( { 'display': 'block' } );
			}
		} );
	}

	// Show the popup if it exists, else redirect

	function showResponse( data, url ) {

		var popup = $( "<div id='wrapper'>" + $.trim( data ) + "</div>" ).find( ".popup" );

		if ( popup.length > 0 ) {

			$( ".overlay .popup" ).html( popup.children( ":first" ) );
			$( ".overlay .popup" ).css( "margin-top", popup.css( "margin-top" ) );
			$( ".overlay" ).css( { 'display': 'block' } );

			handlePopupForm()

		} else {

			window.location.href = url;
		}
	}


	// Handle login/register form submission

	function handlePopupForm() {


		$( ".popup .login.btn" ).click(function(e){

			getPopup( "/user/login" );
			e.preventDefault();
		});

		$(".popup form").submit( function( e ) {

			form = $(this);

			$.ajax( {
				type: "POST",
				url: form.attr('action'),
				data: form.serialize(),
				success: function( data ) {

					showResponse( data, form.attr('action') );
				}
			} );
			e.preventDefault();
		} );
	}

	// Hide popup on escape key press or outside click 

	function hidePopup() {

		$( ".overlay" ).css( { 'display': 'none' } );
	}

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