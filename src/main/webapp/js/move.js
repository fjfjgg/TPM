/**
 * 
 */
"use strict";
var moveDiv = null;
var isDown = false;

function centerDiv(d) {
	var windowy = window.innerHeight || document.documentElement.clientHeight
		|| document.body.clientHeight;
	var windowx = window.innerWidth || document.documentElement.clientWidth
		|| document.body.clientWidth;
	if ((parseInt(windowy) - d.clientHeight) / 2 > 0) {
		d.style.top = (parseInt(windowy) - d.clientHeight) / 2 + "px";
	} else {
		d.style.top = 0;
	}
	if ((parseInt(windowx) - d.clientWidth) / 2 > 0) {
		d.style.left = (parseInt(windowx) - d.clientWidth) / 2 + "px"
	} else {
		d.style.left = 0;
	}
	d.scrollIntoView();
}


function startMove(e) {
	e.preventDefault();
	isDown = true;
	moveDiv = this.parentNode;
	moveDiv.offset = [ moveDiv.offsetLeft - e.clientX, moveDiv.offsetTop - e.clientY ];
}

function stopMove() {
	isDown = false;
	moveDiv = null;
}

function followMove(event) {
	event.preventDefault();
	if (isDown && moveDiv) {
		if (event.clientX + moveDiv.offset[0] > 0)
			moveDiv.style.left = (event.clientX + moveDiv.offset[0]) + 'px';
		if (event.clientY + moveDiv.offset[1] > 0)
			moveDiv.style.top = (event.clientY + moveDiv.offset[1]) + 'px';
	}
	return false;	
}

//Center first dialog
var divList = document.querySelectorAll(".dialog");
for (let i=0; i< divList.length; i++) {
	divList[i].style.position = 'absolute';
	if (i==0) {
		centerDiv(divList[i]);
	}
}

//Start movement
var divcabList = document.querySelectorAll(".dialog h1");
for (let i of divcabList) {
	i.addEventListener('mousedown', startMove, true);
}

//Stop movement
document.addEventListener('mouseup', stopMove, true);

//Move
document.addEventListener('mousemove', followMove, true);
