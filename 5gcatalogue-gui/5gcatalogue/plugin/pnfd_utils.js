/*
* Copyright 2018 Nextworks s.r.l.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

function getAllPnfdInfos(elemId, callback, resId) {
    getJsonFromURL("http://" + catalogueAddr + ":8083/nsd/v1/pnf_descriptors", callback, [elemId, resId]);
}

function getPnfdInfo(pnfdInfoId, callback, elemId) {
    getJsonFromURL("http://" + catalogueAddr + ":8083/nsd/v1/pnf_descriptors/" + pnfdInfoId, callback, [elemId]);
}

function fillPNFDsCounter(data, params) {
    var countDiv = document.getElementById(params[0]);

	//console.log(JSON.stringify(data, null, 4));
	countDiv.innerHTML = data.length;
}

function deletePnfdInfo(pnfdInfoId, resId) {
    deleteRequestToURL("http://" + catalogueAddr + ":8083/nsd/v1/pnf_descriptors/" + pnfdInfoId, showResultMessage, ["PNFD with pnfdInfoID " + pnfdInfoId + " successfully deleted."]);
}

function updatePnfdInfo(pnfdInfoId, elemId) {
    var opState = document.getElementById(elemId).value;

    var jsonObj = JSON.parse("{}");
    var json = JSON.stringify(jsonObj, null, 4);

    console.log("PnfdInfoModifications: " + json);
    patchJsonRequestToURL("http://" + catalogueAddr + ":8083/nsd/v1/pnf_descriptors/" + pnfdInfoId, json, showResultMessage, ["PNFD with pnfdInfoId " + nsdInfoId + " successfully updated."]);
}

function loadPNFDFile(elemId, resId) {
    var files = document.getElementById(elemId).files;

    if (files && files.length > 0) {
        createPnfdInfoId(files[0], resId);
    } else {
        showResultMessage(false, "PNFD file/archive not selected.");
    }
}

function createPnfdInfoId(file, resId) {
    // TODO: handle also userDefinedData
    var jsonObj = {"userDefinedData" : {} };
    var json = JSON.stringify(jsonObj, null, 4);

    postJsonToURL("http://" + catalogueAddr + ":8083/nsd/v1/pnf_descriptors", json, uploadPnfdContent, [file, resId]);
}

function uploadPnfdContent(data, params) {
    //console.log(JSON.stringify(data, null, 4));

    var formData = new FormData();
    formData.append("file", params[0]);
    formData.append("pippo","pluto");
    var pnfdInfoId = data['id'];

    putFileToURL("http://" + catalogueAddr + ":8083/nsd/v1/pnf_descriptors/" + pnfdInfoId + "/pnfd_content", formData, showResultMessage, ["PNFD with pnfdInfoId " + pnfdInfoId + " successfully updated."]);
}

function getDescription(elemId, callback) {
    var pnfdId = getURLParameter('pnfdId');
    console.log(pnfdId);
    getPNFD(pnfdId, elemId, callback);
}

function printDescription(data, params){
    console.log(data);
    var yamlObj = jsyaml.load(data);
    console.log(yamlObj);
    document.getElementById(params[1]).innerHTML = ' - ' + params[0] + ' - ' + yamlObj['description'];
}

function readPNFD(elemId, callback) {
    var pnfdId = getURLParameter('pnfdId');
    console.log(pnfdId);
    getPNFD(pnfdId, elemId, callback);
}

function getPNFD(pnfdInfoId, elemId, callback) {
    getFileFromURL("http://" + catalogueAddr + ":8083/nsd/v1/pnf_descriptors/" + pnfdInfoId + "/pnfd_content", callback, [pnfdInfoId, elemId]);
}

function exportPnfd(pnfdInfoId, resId) {
    postToURL("http://" + catalogueAddr + ":8083/catalogue/cat2catOperation/exportPnfd/" + pnfdInfoId, showResultMessage, ["Request for uploading PNFD with pnfdInfoId " + pnfdInfoId + " successfully submitted to public 5G Catalogue."])
}

function showPnfdGraphCanvas(data,params) {
    console.log(params[0]);
    console.log(params[1]);
    console.log(data)
    console.log(data.lenght);
    for (var i in data) {
        console.log(data[i]['id']);
        if (data[i]['id'] === params[0]){
            var pnfdName= data[i]['pnfdName'];
            console.log(pnfdName);
        }
        document.getElementById("graphOf_"+ data[i]['id']).style.display = "none";
    }

    document.getElementById("graphOf_"+ params[0]).style.display = "block";
    var dataId ='cy_'+params[0];
    console.log("dataid="+dataId);
    document.getElementById(dataId).innerHTML = '<script>' + getPNFD(params[0],dataId, createPNFDGraph); + '</script>';
}

function createPnfdInfosTable(data, params) {
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
        table.innerHTML = '<tr>No PNFDs stored in Catalogue</tr>';
        return;
    }
    var btnFlag = true;

    if (isPublic) {
        console.log("PUBLIC CATALOGUE");
        header = createTableHeaderByValues(['Name', 'Version', 'Designer', 'Onboarding State', 'MANOs', 'Actions'], btnFlag, false);
        cbacks = ['openPNFD_', 'showPnfdGraphCanvas', 'deletePnfdInfo'];
        names = ['View PNFD', 'View PNFD Graph', 'Delete PNFD'];
        columns = [['pnfdName'], ['pnfdVersion'], ['pnfdProvider'], ['pnfdOnboardingState'], ['manosOnboardingStatus']];   
    } else {
        console.log("PRIVATE CATALOGUE");
        header = createTableHeaderByValues(['Name', 'Version', 'Designer', 'Onboarding State', 'MANOs', '5G Catalogues', 'Actions'], btnFlag, false);
        cbacks = ['openPNFD_', 'showPnfdGraphCanvas', 'exportPnfd', 'deletePnfdInfo'];
        names = ['View PNFD', 'View PNFD Graph', 'Upload PNFD', 'Delete PNFD'];
        columns = [['pnfdName'], ['pnfdVersion'], ['pnfdProvider'], ['pnfdOnboardingState'], ['manosOnboardingStatus'], ['c2cOnboardingStatus']]; 
    }

    table.innerHTML = header + '<tbody>';

    var rows = '';
    for (var i in data) {
        rows +=  createPnfdInfosTableRow(data[i], btnFlag, cbacks, names, columns, resId);
    }

    table.innerHTML += rows + '</tbody>';
}

function createPnfdInfosTableRow(data, btnFlag, cbacks, names, columns, resId) {
    //console.log(JSON.stringify(data, null, 4));

    var text = '';
    var btnText = '';
    if (btnFlag) {
        btnText += createLinkSet(data['id'], resId, names, cbacks);
        cretePNFDViewModal(data['id'], "pnfdViewModals");
    }

  	text += '<tr>';
  	for (var i in columns) {
  	    var values = [];
  	    getValuesFromKeyPath(data, columns[i], values);
  	    //console.log(values);

  	    var subText = '<td>';
  	    var subTable = '<table class="table table-bordered">';

  	    if (data.hasOwnProperty(columns[i][0])) {
            if(values instanceof Array && values.length > 1) {
                for (var v in values) {
                    subTable += '<tr><td>' + values[v] + '</td><tr>';
                }
                subText += subTable + '</table>';
            } else if (values[0] instanceof Object) {
                var acks = values[0];
                $.each(acks, function(key, value) {
                    subTable += '<tr><td>'+ key + '</td><td>' + value + '</td><tr>';
                });
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

function cretePNFDViewModal(pnfdInfoId, modalsContainerId) {

    //console.log('Creating view modal for pnfdInfoId: ' + pnfdInfoId);
    var container = document.getElementById(modalsContainerId);

    if (container) {
        var text = '<div id="openPNFD_' + pnfdInfoId + '" class="modal fade bs-example-modal-lg" tabindex="-1" role="dialog" aria-hidden="true" style="display: none;">\
                    <div class="modal-dialog modal-lg">\
                      <div class="modal-content">\
                        <div class="modal-header">\
                            <button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">×</span>\
                            </button>\
                            <h4 class="modal-title" id="myModalLabel">PNFD with pnfdInfoId: ' + pnfdInfoId + '</h4>\
                        </div>\
                        <div class="modal-body">\
                            <textarea id="viewPNFDContent_' + pnfdInfoId + '" class="form-control" rows="30" readonly></textarea>\
                        </div>\
                        <div class="modal-footer">\
                            <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>\
                        </div>\
                      </div>\
                    </div>\
                </div>';

            container.innerHTML += text;
            getPNFD(pnfdInfoId, 'viewPNFDContent_' + pnfdInfoId, fillPNFDViewModal);
    }
}

function fillPNFDViewModal(data, params) {

    var yamlObj = jsyaml.load(data);
    //console.log(yamlObj);

    var yaml = jsyaml.dump(data, {
        indent: 4,
        styles: {
        '!!int'  : 'decimal',
        '!!null' : 'camelcase'
        }
    });

    //console.log(yaml);
    var pnfdInfoId = params[0];
    var textArea = document.getElementById(params[1]);
    textArea.value = yaml;
}