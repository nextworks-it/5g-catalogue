
function getAllNsdInfos(elemId, callback, resId) {
    getJsonFromURL("http://" + catalogueAddr + ":8083/nsd/v1/ns_descriptors", callback, [elemId, resId]);
}

function getNsdInfo(nsdInfoId, callback, elemId) {
    getJsonFromURL("http://" + catalogueAddr + ":8083/nsd/v1/ns_descriptors/" + nsdInfoId, callback, [elemId]);
}

function fillNSDsCounter(data, params) {
    var countDiv = document.getElementById(params[0]);
	
	//console.log(JSON.stringify(data, null, 4));
	countDiv.innerHTML = data.length;
}

function deleteNsdInfo(nsdInfoId, resId) {
    deleteRequestToURL("http://" + catalogueAddr + ":8083/nsd/v1/ns_descriptors/" + nsdInfoId, showResultMessage, ["NSD with nsdInfoID " + nsdInfoId + " successfully deleted."]);
}

function updateNsdInfo(nsdInfoId, elemId) {
    var opState = document.getElementById(elemId).value;
    
    var jsonObj = JSON.parse("{}");
    jsonObj['nsdOperationalState'] = opState;
    var json = JSON.stringify(jsonObj, null, 4);
    
    console.log("NsdInfoModifications: " + json);
    patchJsonRequestToURL("http://" + catalogueAddr + ":8083/nsd/v1/ns_descriptors/" + nsdInfoId, json, showResultMessage, ["NSD with nsdInfoId " + nsdInfoId + " successfully updated."]);
}

function loadNSDFile(elemId, resId) {
    var files = document.getElementById(elemId).files;
    
    if (files && files.length > 0) {
        createNsdInfoId(files[0], resId);
    } else {
        showResultMessage(false, "NSD file/archive not selected.");
    }
}

function createNsdInfoId(file, resId) {
    // TODO: handle also userDefinedData
    var jsonObj = {"userDefinedData" : {} };
    var json = JSON.stringify(jsonObj, null, 4);
    
    postJsonToURL("http://" + catalogueAddr + ":8083/nsd/v1/ns_descriptors", json, uploadNsdContent, [file, resId]);
}

function uploadNsdContent(data, params) {
    console.log(JSON.stringify(data, null, 4));
    
    var formData = new FormData();
    formData.append("file", params[0]);
    formData.append("pippo","pluto");
    var nsdInfoId = data['id'];
    
    putFileToURL("http://" + catalogueAddr + ":8083/nsd/v1/ns_descriptors/" + nsdInfoId + "/nsd_content", formData, showResultMessage, ["NSD with nsdInfoId " + nsdInfoId + " successfully updated."]);
}

function getNSD(nsdInfoId, elemId, callback) {
    getFileFromURL("http://" + catalogueAddr + ":8083/nsd/v1/ns_descriptors/" + nsdInfoId + "/nsd_content", callback, [nsdInfoId, elemId]);
}

function createNsdInfosTable(data, params) {
    //console.log(JSON.stringify(data, null, 4));
    //console.log(params);

    var tableId = params[0];
    var resId = params[1];
    var table = document.getElementById(tableId);
    if (!table) {
        return;
    }
    if (!data || data.length == 0) {
	//console.log('No NS Instances');
        table.innerHTML = '<tr>No NSDs stored in Catalogue</tr>';
        return;
    }
    var btnFlag = true;
    var header = createTableHeaderByValues(['Name', 'Version', 'Designer', 'Operational State', 'Onboarding State'], btnFlag, false);
    var cbacks = ['openNSD_', 'updateNsdInfo_', 'deleteNsdInfo'];
    var names = ['View NSD', 'Enable/Disable NSD', 'Delete NSD'];
    var columns = [['nsdName'], ['nsdVersion'], ['nsdDesigner'], ['nsdOperationalState'], ['nsdOnboardingState']];

    table.innerHTML = header + '<tbody>';

    var rows = '';
    for (var i in data) {
        rows +=  createNsdInfosTableRow(data[i], btnFlag, cbacks, names, columns, resId);
    }
    
    table.innerHTML += rows + '</tbody>';
}

function createNsdInfosTableRow(data, btnFlag, cbacks, names, columns, resId) {
    //console.log(JSON.stringify(data, null, 4));

    var text = '';
    var btnText = '';
    if (btnFlag) {
        btnText += createActionButton(data['id'], resId, names, cbacks);
        createUpdateNsdInfoModal(data['id'], data['nsdOperationalState'], "updateNsdInfosModals");
        creteNSDViewModal(data['id'], "nsdViewModals");
    }

	text += '<tr>';
	for (var i in columns) {
	    var values = [];
	    getValuesFromKeyPath(data, columns[i], values);
	    //console.log(values);

	    var subText = '<td>';
	    var subTable = '<table class="table table-borderless">';

	    if (data.hasOwnProperty(columns[i][0])) {
            if(values instanceof Array && values.length > 1) {
                for (var v in values) {
                    subTable += '<tr><td>' + values[v] + '</td><tr>';
                }
                subText += subTable + '</table>';
            } else {
                subText += values;
            }
	    }
	    subText += '</td>';
	    text += subText;
	}
	text += btnText + '</tr>';
    
    return text;
}

function creteNSDViewModal(nsdInfoId, modalsContainerId) {
              
    console.log('Creating view modal for nsdInfoId: ' + nsdInfoId);
    var container = document.getElementById(modalsContainerId);
    
    if (container) {
        var text = '<div id="openNSD_' + nsdInfoId + '" class="modal fade bs-example-modal-lg" tabindex="-1" role="dialog" aria-hidden="true" style="display: none;">\
                    <div class="modal-dialog modal-lg">\
                      <div class="modal-content">\
                        <div class="modal-header">\
                          <button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">×</span>\
                          </button>\
                          <h4 class="modal-title" id="myModalLabel">NSD with nsdInfoId: ' + nsdInfoId + '</h4>\
                        </div>\
                        <div class="modal-body">\
                            <textarea id="viewNSDContent_' + nsdInfoId + '" class="form-control" rows="30" readonly></textarea>\
                        </div>\
                        <div class="modal-footer">\
                          <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>\
                          <!--button type="button" class="btn btn-primary" data-dismiss="modal" onclick=updateNsdInfo("' + nsdInfoId + '","ed_' + nsdInfoId + '");>Submit</button-->\
                        </div>\
                      </div>\
                    </div>\
                </div>';
        
            container.innerHTML += text;          
            getNSD(nsdInfoId, 'viewNSDContent_' + nsdInfoId, fillNSDViewModal);
    }  
}

function fillNSDViewModal(data, params) {
    
    //var yamlObj = jsyaml.load(data);
    
    var yaml = jsyaml.dump(data, {
        indent: 4,
        styles: {
        '!!int'  : 'decimal',
        '!!null' : 'camelcase'
        }
    });
    
    //console.log(yaml);
    var nsdInfoId = params[0];
    var textArea = document.getElementById(params[1]);
    textArea.value = yaml;
}

function createUpdateNsdInfoModal(nsdInfoId, opState, modalsContainerId) {
    
    var container = document.getElementById(modalsContainerId);
    
    if (container) {
        var text = '<div id="updateNsdInfo_' + nsdInfoId + '" class="modal fade bs-example-modal-sm" tabindex="-1" role="dialog" aria-hidden="true" style="display: none;">\
                    <div class="modal-dialog modal-lg">\
                      <div class="modal-content">\
                        <div class="modal-header">\
                          <button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">×</span>\
                          </button>\
                          <h4 class="modal-title" id="myModalLabel">Enable / Disable NSD</h4>\
                        </div>\
                        <div class="modal-body">\
                            <form class="form-horizontal form-label-left">\
								<div class="form-group">\
									<label class="control-label col-md-3 col-sm-3 col-xs-12">Operational State</label>\
									<div class="col-md-9 col-sm-9 col-xs-12">\
										<select id="ed_' + nsdInfoId + '" class="form-control">';
        if (opState == 'ENABLED') {
            text += '<option value="ENABLED">ENABLED</option>\
					<option value="DISABLED">DISABLED</option>';
        } else {
            text += '<option value="DISABLED">DISABLED</option>\
                    <option value="ENABLED">ENABLED</option>';
        }
		text += '</select>\
									</div>\
								</div>\
							</form>\
                        </div>\
                        <div class="modal-footer">\
                          <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>\
                          <button type="button" class="btn btn-primary" data-dismiss="modal" onclick=updateNsdInfo("' + nsdInfoId + '","ed_' + nsdInfoId + '");>Submit</button>\
                        </div>\
                      </div>\
                    </div>\
                </div>';
        
        container.innerHTML += text;
    }
}
