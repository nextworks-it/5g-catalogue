
var catalogueAddr = '10.0.8.44';
var cataloguePort = '8083';

var stopRefreshing = false;

function refresh(btnFlag) {
	if(stopRefreshing && !Boolean(btnFlag)) {
//		console.log("stop refreshing: "+ stopRefreshing);
		return;
	}
	$(document).ready(function () {
		for (i = 0; i < document.forms.length; i++) {
	        document.forms[i].reset();
	    }
	});
	location.reload();
	scrollPageTo(0);
}

function scrollPageTo(offset) {
    // For Chrome, Safari and Opera
	var body = document.body;
	// Firefox and IE places the overflow at the <html> level, unless else is specified.
	// Therefore, we use the documentElement property for these two browsers
	var html = document.documentElement;
	if (body)
        body.scrollTop = offset;
	if (html)
        html.scrollTop = offset;
}

function clearForms(parentId, flag) {
	//console.log(parentId);
	var elems = $('#' + parentId).find('form');
	//console.log(elems.length)
	for (var i = 0; i < elems.length; i++) {
		//console.log(elems[i])
		elems[i].reset();
	}
	
	if (flag) {
		var modal = document.getElementById(parentId);
		modal.style = 'display:none';
	}
}

function getURLParameter(name) {
	return decodeURIComponent((new RegExp('[?|&]' + name + '=' + '([^&;]+?)(&|#|;|$)')
							.exec(location.search) || [ , "" ])[1]
							.replace(/\+/g, '%20'))	|| null;
}

function showResultMessage(success, msg) {
    var flag = "true";
    var elem = document.getElementById("response");
    var text;
	if (success == null) {
		text = '<div class="alert alert-warning alert-dismissible fade in" role="alert">\
                <button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">×</span>\
                </button>\
				<strong>WARNING!</strong> ' + msg + '</div>';
	} else {
		if (success) {
			text = '<div class="alert alert-info alert-dismissible fade in" role="alert">\
					<button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">×</span>\
					</button>\
					<strong>SUCCESS!</strong> ' + msg + '</div>';
		} else {
			text =  '<div class="alert alert-danger alert-dismissible fade in" role="alert">\
					<button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">×</span>\
					</button>\
					<strong>ERROR!</strong> ' + msg + '</div>';
		}
	}
	text += '<button class="btn btn-info" onclick="refresh(' + flag + ');"><i class="fa fa-refresh"></i> Refresh</button></div>';
    elem.innerHTML = text;	
	scrollPageTo(elem.offsetTop);
}

function loadFromFile(type, evt, elementId, resId) {
    //Retrieve the first (and only!) File from the FileList object
    var file = evt.target.files;
	console.log(file[0]);
    if (file.length < 1)
		return;
	if (type == 'ZIP') {
		readZipFile(file[0], elementId, resId);
	} else {
		console.log("Json File in div: " + elementId);
		var elem = document.getElementById(elementId);
		console.log("Json File in div: " + elementId + " " + elem);
		if(!elem)
			return;
		openTextFile(evt, elem);
	}	
}

function readZipFile(file, containerId, resId) {
	console.log(file);
	zip.workerScripts = {
		deflater: ['../../plugins/zip_js/z-worker.js', '../../plugins/zip_js/deflate.js'],
		inflater: ['../../plugins/zip_js/z-worker.js', '../../plugins/zip_js/inflate.js']
	};
	var model = (function() {
		var URL = this.webkitURL || this.mozURL || this.URL;
	
		return {
			getEntries : function(file, onend) {
				zip.createReader(new zip.BlobReader(file), function(zipReader) {
					zipReader.getEntries(onend);
					//console.log("onend: " + onend);
				}, onerror);
			},
			getEntryFile : function(entry, onend, onprogress) {
				var writer, zipFileEntry;	
				function getData() {
					entry.getData(writer, function(blob) {
						var blobURL = URL.createObjectURL(blob);
						onend(blobURL);
					}, onprogress);
				}
				writer = new zip.BlobWriter();
				getData();				
			}
		};
	})();	
	model.getEntries(file, function(entries) {
		var cnt = 0;
		entries.forEach(function(entry) {
			if(entry.filename.indexOf('script')<0)
				cnt++;
		});
		if(cnt != 1) {
			onerror('Wrong package format');
			return false;
		}
		entries.forEach(function(entry) {
//			console.log('filename: ' + entry.filename);
			model.getEntryFile(entry, function(blobURL) {
				var subdiv = document.createElement('div');
				$(subdiv).load(blobURL, null, function() {
					if(entry.filename.indexOf('.json')<0)
						return true;
					subdiv.id = entry.filename;
					var container = document.getElementById(containerId);
					container.appendChild(subdiv);
				});
			}, function(current, total) {
			});
		});
	});
	
	function onerror(message) {
		if(!message || message == undefined)
			message = 'Error';
		console.log(message);
		showResultMessage(false, resId, message);
	}
}

function openTextFile(event, container) {
	var file = event.target.files[0];
	console.log("File: " + file);
	if (!file) {
		alert("Invalid file");
		return;
	}
//  console.log("type = " + f.type);
	var reader = new FileReader();
	
	reader.onload = function(event) {
		console.log("File read: " + event.target.result);
		container.innerHTML = event.target.result;
		console.log(container.innerHTML);
	};
	
	reader.readAsText(file);
}

function createTableHeaderFromObject(data, btnFlag, cols) {
	// creating header
	var text = '<thead><tr>';
	if(typeof(data) == 'object') {
        // data length has already been checked
		$.each(data, function(key, val) {
			if (cols && cols.indexOf(key) < 0)
				return true;
            if (key.indexOf('password') >= 0)
				return true;
			
			text += '<th>' + key + '</th>';
		});
		if (btnFlag) {
			text += '<th></th>';
		}
	}
	text += '</tr></thead>';
    
	return text;
}

function createTableHeaderByValues(cols, btnFlag, checkbFlag) {
    var text = '<thead><tr>';
	
	if (cols) {
		if(checkbFlag) {
			text +='<th><input type="checkbox" class="checkbox-toggle purge-check" style="display: none"></th>'
			//<button type="button" class="btn btn-default btn-sm checkbox-toggle purge-check" style="display: none"><i class="fa fa-square-o"></i></button></th>'
		}
		var value;
		for (value in cols) {
            text += '<th>' + cols[value] + '</th>';
        }
		if (btnFlag) {
			text += '<th></th>';
		}
    }
	text += '</tr></thead>';
	return text;
}

function hideElement(elem, flag) {
	if (flag) {
		elem.style.display = 'none';	
	} else {
		if(elem.tagName.toLowerCase() == 'table')
			elem.style.display = 'table';
		else
			elem.style.display = 'block';
	}
}

function getValuesFromKeyPath(data, keys, result) {
    if(keys.length <= 1) {
        if (data instanceof Array) {
            for (k=0; k<data.length; k++) {
                    if (data[k].hasOwnProperty(keys[0])) {
                        result.push(data[k][keys[0]]);
                    }
            }
        } else {
                if (data && data.hasOwnProperty(keys[0])) {
                    result.push(data[keys[0]]);
                }
        }
    } else {
        var newKeys = [];
        for (h=1; h<keys.length; h++) {
            newKeys.push(keys[h]);
        }
        if (data instanceof Array) {
            for (k=0; k<data.length; k++) {
                if (data[k].hasOwnProperty(keys[0])) {
                    getValuesFromKeyPath(data[k][keys[0]], newKeys, result);
                }
            }
        } else {
            if (data.hasOwnProperty(keys[0])) {
                getValuesFromKeyPath(data[keys[0]], newKeys, result);
            }
        }
    }
}

function createActionButton(id, resId, btnNames, btnCallbacks) {
	var text = '<td>';
	if(btnNames instanceof Array) {
		if(btnNames.length == btnCallbacks.length) {
			text += createDropdownButton(id, resId, btnNames, btnCallbacks);
		}		
	} else {
		text +=  createButton(id, resId, btnNames, btnCallbacks);
	}
	text += '</td>';	
	return text;
}

function createButton(id, resId, btnName, btnCallback) {
	
	var text = 	'<button type="button" class="btn btn-info btn-sm btn-block';
	if (btnCallback.toLowerCase().indexOf("delete") >= 0 ||
		btnCallback.toLowerCase().indexOf("get") >= 0) {
        text += '" onclick=' + btnCallback + '("' + id + '","' + resId + '")>';
    } else if (btnCallback.toLowerCase().indexOf("view") >= 0) {
        text += '" onclick="location.href=\'' + btnCallback + id + '\'">';
    } else if (btnCallback.toLowerCase().indexOf("open") >= 0 ||
			   btnCallback.toLowerCase().indexOf("update") >= 0) {
			text += ' buttonModal_'+ btnCallback + '" data-toggle="modal" data-target="#' + btnCallback + id + '" data-id="' + id + '">';
    }  else if (btnName.toLowerCase().indexOf("graph") >= 0) { // condizione aggiunta
		text += '" onclick="location.href=\'' + btnCallback + id + '\'">';
	}
	text += btnName + '</button>';
//	console.log("button: \n" + text);
    
	return text;
}

function createDropdownButton(id, resId, btnNames, btnCallbacks) {
	var text = '<div class="btn-group">\
				<button type="button" class="btn btn-primary dropdown-toggle" data-toggle="dropdown">Action\
				<span class="fa fa-caret-down"></span>\
				</button><ul class="dropdown-menu" role="menu">'; //style="position: static;"
	for(var i=0; i<btnNames.length; i++) {
		text += '<li>' + createButton(id, resId, btnNames[i], btnCallbacks[i]) + '</li>';
		console.log(btnNames[i]);
	}
	text += '</ul></div>';		//<input type="text" class="form-control">
	
	return text;
}
