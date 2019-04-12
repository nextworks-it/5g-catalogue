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
    //console.log(JSON.stringify(data, null, 4));

    var formData = new FormData();
    formData.append("file", params[0]);
    formData.append("pippo","pluto");
    var nsdInfoId = data['id'];

    putFileToURL("http://" + catalogueAddr + ":8083/nsd/v1/ns_descriptors/" + nsdInfoId + "/nsd_content", formData, showResultMessage, ["NSD with nsdInfoId " + nsdInfoId + " successfully updated."]);
}

function getDescription(descrId) {
    var nsdId = getURLParameter('nsdId');
    console.log(nsdId);
    getNSD(nsdId, descrId, printDescription);
}

function printDescription(data, params){
    console.log(data);
    var yamlObj = jsyaml.load(data);
    console.log(yamlObj);
    document.getElementById(params[1]).innerHTML = ' - ' + params[0] + ' - ' + yamlObj['description'];
}

function readNSD(graphId) {
    var nsdId = getURLParameter('nsdId');
    console.log(nsdId);
    getNSD(nsdId, graphId, createNSDGraph);
}

function getNSD(nsdInfoId, elemId, callback) {
    getFileFromURL("http://" + catalogueAddr + ":8083/nsd/v1/ns_descriptors/" + nsdInfoId + "/nsd_content", callback, [nsdInfoId, elemId]);
}

function showNsdGraphCanvas(data,params) {
    console.log(params[0]);
    console.log(params[1]);
    console.log(data)
    console.log(data.lenght);
    for (var i in data) {
        console.log(data[i]['id']);
        if (data[i]['id'] === params[0]){
            var nsdName= data[i]['nsdName'];
            console.log(nsdName);
        }
        document.getElementById("graphOf_"+ data[i]['id']).style.display = "none";
    }

    document.getElementById("graphOf_"+ params[0]).style.display = "block";
    var dataId ='cy_'+params[0];
    console.log("dataid="+dataId);
    document.getElementById(dataId).innerHTML = '<script>'+getNSD(params[0],dataId, createNSDGraph);+'</script>';
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
    var header = createTableHeaderByValues(['Name', 'Version', 'Designer', 'Operational State', 'Onboarding State', 'MANOs Onboarding State', 'Actions'], btnFlag, false);
    var cbacks = ['openNSD_', 'showNsdGraphCanvas', 'updateNsdInfo_', 'deleteNsdInfo'];
    var names = ['View NSD', 'View NSD Graph', 'Change NSD OpState', 'Delete NSD'];
    var columns = [['nsdName'], ['nsdVersion'], ['nsdDesigner'], ['nsdOperationalState'], ['nsdOnboardingState'], ['manosOnboardingStatus']];

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
        //btnText += createActionButton(data['id'], resId, names, cbacks);
        btnText += createLinkSet(data['id'], resId, names, cbacks);
        createUpdateNsdInfoModal(data['id'], data['nsdOperationalState'], "updateNsdInfosModals");
        creteNSDViewModal(data['id'], "nsdViewModals");
        createCanvas(data);
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
                var manoAcks = values[0];
                $.each(manoAcks, function(key, value) {
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

function createCanvas(data){
    console.log(data);
    console.log('Canvas per grafico' +  data['id']);
    var dataId ='cy_'+data['id'];
    var nsdName=data['nsdName'];
    var graphName="graphOf_" + data['id'];
    document.getElementById("nsdGraphCanvas").innerHTML += '<div id="' + graphName + '" class="graph_canvas" style="display: none";>\
                                                            <div class="col-md-12">\
                                                            <div class="x_panel">\
                                                                <div class="x_title">\
                                                                <h2>NETWORK SERVICE DESCRIPTOR GRAPH - ' + nsdName + ' <small></small></h2>\
                                                                <div class="clearfix"></div>\
                                                                </div>\
                                                                <div class="x_content">\
                                                                <style>\
                                                                    #' + dataId + '{\
                                                                    min-height: 600px;\
                                                                    width: auto;\
                                                                    left: 0;\
                                                                    top: 0;\
                                                                    }\
                                                                </style>\
                                                                <div id="'+dataId+'"></div>\
                                                                </div>\
                                                            </div>\
                                                            </div>\
                                                        </div>';

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

    var yamlObj = jsyaml.load(data);
    console.log(yamlObj);

    var yaml = jsyaml.dump(data, {
        indent: 4,
        styles: {
        '!!int'  : 'decimal',
        '!!null' : 'camelcase'
        }
    });

    console.log(yaml);
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

function createNSDGraph(data, params) {
    console.log(params[1]);
    var yamlObj = jsyaml.load(data);
    console.log(yamlObj);
    var nodeTempl=yamlObj['topologyTemplate']['nodeTemplates'];
    //console.log(nodeTempl);

    var nodes = [];
    var edges = [];

    $.each(nodeTempl, function(key, value){
        //console.log(key + " " + value['type']);
        if (value['type'] === 'tosca.nodes.nfv.NsVirtualLink'){
            nodes.push({ group: 'nodes', data: { id: key, name: 'Link - ' + key, label: 'Link - ' + key, weight: 50, faveColor: '#fff', faveShape: 'ellipse' }, classes: 'bottom-center net'});
        }
        //console.log(nodes);
    });

    $.each(nodeTempl, function(key, value){
        //console.log(key + " " + value['type']);
        if (value['type'] === 'tosca.nodes.nfv.VNF'){
            nodes.push({ group: 'nodes', data: { id: key, name: 'NODE - ' + key, label: 'NODE - ' + key, weight: 70, faveColor: '#fff', faveShape: 'ellipse' }, classes: 'bottom-center vnf'});
            $.each( value['requirements']['virtualLink'], function(key1, value1){
                //console.log(key1 + " " + value1.split('/')[0]);
                //console.log(key);
                edges.push({ group: 'edges', data: { source: key, target: value1.split('/')[0], faveColor: '#706f6f', strength: 70 }});
                //console.log(nodes);
            });
        }
        //console.log(nodes);
    });

    $.each(nodeTempl, function(key, value){
        //console.log(key + " " + value['type']);
        if (value['type'] === 'tosca.nodes.nfv.PNF'){
            nodes.push({ group: 'nodes', data: { id: key, name: 'NODE - ' + key, label: 'NODE - ' + key, weight: 70, faveColor: '#fff', faveShape: 'ellipse' }, classes: 'bottom-center pnf'});
            $.each( value['requirements']['virtualLink'], function(key1, value1){
                //console.log(key1 + " " + value1.split('/')[0]);
                //console.log(key);
                edges.push({ group: 'edges', data: { source: key, target: value1.split('/')[0], faveColor: '#706f6f', strength: 70 }});
                //console.log(nodes);
            });
        }
        //console.log(nodes);
    });

    var cy = cytoscape({
        container: document.getElementById(params[1]),

		layout: {
			name: 'cose',
			padding: 10,
		},

		style: cytoscape.stylesheet()
			.selector('node')
				.css({
					'shape': 'data(faveShape)',
					'content': 'data(name)',
					'text-valign': 'center',
					'text-outline-width': 0,
					'text-width': 2,
					//'text-outline-color': '#000',
					'background-color': 'data(faveColor)',
					'color': '#000',
					'label': 'data(name)'
				})
			.selector(':selected')
				.css({
					'border-width': 3,
					'border-color': '#333'
				})
			.selector('edge')
				.css({
					'curve-style': 'bezier',
					'opacity': 0.666,
					'width': 'mapData(strength, 70, 100, 2, 6)',
					'target-arrow-shape': 'circle',
					'source-arrow-shape': 'circle',
					'line-color': 'data(faveColor)',
					'source-arrow-color': 'data(faveColor)',
					'target-arrow-color': 'data(faveColor)'
				})
			.selector('edge.questionable')
				.css({
					'line-style': 'dotted',
					'target-arrow-shape': 'diamond',
					'source-arrow-shape': 'diamond'
				})
			.selector('.vnf')
				.css({
					'background-image': '/5gcatalogue/images/vnf_icon_80.png',
					'width': 80,//'mapData(weight, 40, 80, 20, 60)',
					'height': 80
				})
			.selector('.pnf')
				.css({
					'background-image': '/5gcatalogue/images/pnf_icon_80.png',
					'width': 80,//'mapData(weight, 40, 80, 20, 60)',
					'height': 80
				})
			.selector('.net')
				.css({
					'background-image': '/5gcatalogue/images/net_icon_50.png',
					'width': 50,//'mapData(weight, 40, 80, 20, 60)',
					'height': 50
				})
			.selector('.sap')
				.css({
					'background-image': '/5gcatalogue/images/sap_icon_grey_50.png',
					'width': 50,//'mapData(weight, 40, 80, 20, 60)',
					'height': 50
				})
			.selector('.faded')
				.css({
					'opacity': 0.25,
					'text-opacity': 0
				})
			.selector('.top-left')
				.css({
					'text-valign': 'top',
					'text-halign': 'left'
				})
			.selector('.top-right')
				.css({
					'text-valign': 'top',
					'text-halign': 'right'
				})
			.selector('.bottom-center')
				.css({
					'text-valign': 'bottom',
					'text-halign': 'center'
				}),

		elements: {
		  nodes: nodes,
		  edges: edges
		},

		ready: function(){
		  window.cy = this;
		}
	});

	cy.minZoom(0.5);
	cy.maxZoom(1.6);
}
