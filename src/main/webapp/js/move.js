/**
 * 
 */
"use strict";
let moveDiv = null;
let isDown = false;
const ongoingTouches = [];

function centerDiv(d) {
	let windowy = window.innerHeight || document.documentElement.clientHeight
		|| document.body.clientHeight;
	let windowx = window.innerWidth || document.documentElement.clientWidth
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
	moveDiv.offset = [moveDiv.offsetLeft - e.clientX, moveDiv.offsetTop - e.clientY];
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

function handleTouchStart(evt) {
	for (const touch of evt.changedTouches) {
		ongoingTouches.push(copyTouch(touch));
		if (ongoingTouchIndexById(touch.identifier) == 0) {
			isDown = true;
			moveDiv = this.parentNode;
			moveDiv.offset = [moveDiv.offsetLeft - touch.clientX, moveDiv.offsetTop - touch.clientY];
		}
	}
}

function handleTouchMove(evt) {
	for (const touch of evt.changedTouches) {
		const idx = ongoingTouchIndexById(touch.identifier);
		if (idx >= 0) {
			ongoingTouches.splice(idx, 1, copyTouch(touch)); // swap in the new touch record
			if (idx == 0 && isDown && moveDiv) {
				evt.preventDefault();
				if (touch.clientX + moveDiv.offset[0] > 0)
					moveDiv.style.left = (touch.clientX + moveDiv.offset[0]) + 'px';
				if (touch.clientY + moveDiv.offset[1] > 0)
					moveDiv.style.top = (touch.clientY + moveDiv.offset[1]) + 'px';
			}
		}
	}
}

function handleTouchEnd(evt) {
	for (const touch of evt.changedTouches) {
		let idx = ongoingTouchIndexById(touch.identifier);
		if (idx >= 0) {
			ongoingTouches.splice(idx, 1); // remove it; we're done
			if (idx == 0) {
				console.log("touch end");
				stopMove();
			}
		}
	}
}

function handleTouchCancel(evt) {
	for (const touch of evt.changedTouches) {
		let idx = ongoingTouchIndexById(touch.identifier);
		ongoingTouches.splice(idx, 1); // remove it; we're done
		if (idx == 0) {
			evt.preventDefault();
			stopMove();
		}
	}
}

function copyTouch({ identifier, pageX, pageY }) {
	return { identifier, pageX, pageY };
}

function ongoingTouchIndexById(idToFind) {
	for (let i = 0; i < ongoingTouches.length; i++) {
		const id = ongoingTouches[i].identifier;
		if (id === idToFind) {
			return i;
		}
	}
	return -1; // not found
}

function startup() {
	//Center
	let divList = document.querySelectorAll(".dialog");
	for (let i=0; i< divList.length; i++) {
		divList[i].style.visibility="hidden";
		divList[i].style.position = 'absolute';
		if (i==0) {
			//Center first dialog
			centerDiv(divList[i]);
		}
		divList[i].style.visibility="visible";
	}
	let divcabList = document.querySelectorAll('.dialog h1');
	for (let a of divcabList) {
		//Start movement
		a.addEventListener('mousedown', startMove, true);
		a.addEventListener("touchstart", handleTouchStart);
		a.addEventListener("touchend", handleTouchEnd);
		a.addEventListener("touchcancel", handleTouchCancel);
		a.addEventListener("touchmove", handleTouchMove);
	}
	//Stop movement
	document.addEventListener('mouseup', stopMove, true);
	//Move
	document.addEventListener('mousemove', followMove, true);
}

document.addEventListener("DOMContentLoaded", startup);

