function jump(time)
{
	var video = document.getElementsByTagName('video')[0];
	if (video!=undefined){
		video.currentTime=time;
		video.play();
	} else {	
		document.getElementsByTagName('audio')[0].currentTime=time;
		document.getElementsByTagName('audio')[0].play();
	}
}
function showHideTier(box,id) 
{
	var arr = new Array();				
	arr = document.getElementsByName(id);
	for (var i = 0; i < arr.length; i++) {
		var elm = document.getElementsByName(id).item(i);
		elm.style.display = box.checked? "table-row":"none"
	}				
}	