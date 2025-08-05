function showModal(e) {
	e.preventDefault();
    e.stopPropagation();
    
    let captionText = document.getElementById("tool-modal-caption");
    document.getElementById('tool-modal').style.display = "block";
    captionText.textContent = this.dataset.name;
    // si existe un textarea oculto con el id adecuado, los datos se cargan de él,
	// si no se piden con ajax
	let modalSrc = document.getElementById("toolframe");
	if (modalSrc) {
		modalSrc.src = this.getAttribute("href");
    }
    return false;
}

function closeModal(e) {
	let smodal = document.getElementById('tool-modal');
	if (e instanceof KeyboardEvent){
  		if (e.code == 'Escape') {
            smodal.style.display = "none";
        }
	} else {
		smodal.style.display = "none";
	}
}

function configModal() {
    // Modales
    let smodal = document.getElementById('tool-modal');
    if (!smodal) {
    	return;
	}
    // Enlaces 
    let enlaces = document.getElementsByClassName('a-tool-modal');
    for (let ei of enlaces) {
        ei.setAttribute('disabled', 'disabled');
        ei.addEventListener('click', showModal);
    }
    
    //Cerrar haciendo clien en X o pulsando ESC
    // Get the <span> element that closes the modal
    let spans = document.getElementsByClassName("modal-close");
    for (let si of spans ) {
        si.onclick = closeModal;
    }

    // Handle ESC key (key code 27)
    document.addEventListener('keyup', closeModal); 

    let modalSrcContent = document.getElementById("tool-modal-content");
    modalSrcContent.style.height="auto";
}



window.addEventListener("load", function() {
	configModal();
});

window.addEventListener("message",
	() => { closeModal(); });

