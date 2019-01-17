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

function getAllVnfInfos(elemId, callback, resId) {
    console.log("**************************Sto facendo la getallvnfinfo*********************");
    getJsonFromURL("http://" + catalogueAddr + ":8083/vnfpkgm/v1/vnf_packages", callback, [elemId, resId]);
}

function getVnfInfo(vnfInfoId, callback, elemId) {
    getJsonFromURL("http://" + catalogueAddr + ":8083/vnfpkgm/v1/vnf_packages/" + vnfInfoId, callback, [elemId]);
}

function fillVNFsCounter(data, params) {
    var countDiv = document.getElementById(params[0]);
	
	//console.log(JSON.stringify(data, null, 4));
	countDiv.innerHTML = data.length;
}

function deleteVnfInfo(vnfInfoId, resId) {
    deleteRequestToURL("http://" + catalogueAddr + ":8083/vnfpkgm/v1/vnf_packages/" + vnfInfoId, showResultMessage, ["VNF with vnfInfoID " + vnfInfoId + " successfully deleted."]);
}

function updateVnfInfo(vnfInfoId, elemId) {
    var opState = document.getElementById(elemId).value;
    
    var jsonObj = JSON.parse("{}");
    jsonObj['operationalState'] = opState;
    var json = JSON.stringify(jsonObj, null, 4);
    
    console.log("VnfInfoModifications: " + json);
    patchJsonRequestToURL("http://" + catalogueAddr + ":8083/vnfpkgm/v1/vnf_packages/" + vnfInfoId, json, showResultMessage, ["VNF with vnfInfoId " + vnfInfoId + " successfully updated."]);
}

function loadVNFFile(elemId, resId) {
    var files = document.getElementById(elemId).files;
    
    if (files && files.length > 0) {
        createVnfInfoId(files[0], resId);
    } else {
        showResultMessage(false, "VNF file/archive not selected.");
    }
}

function createVnfInfoId(file, resId) {
    // TODO: handle also userDefinedData
    var jsonObj = {"userDefinedData" : {} };
    var json = JSON.stringify(jsonObj, null, 4);
    console.log(json)
    postJsonToURL("http://" + catalogueAddr + ":8083/vnfpkgm/v1/vnf_packages", json, uploadVnfContent, [file, resId]);
}

function uploadVnfContent(data, params) {
    console.log(JSON.stringify(data, null, 4));
    
    var formData = new FormData();
    formData.append("file", params[0]);
    formData.append("pippo","pluto");
    var vnfInfoId = data['id'];
    
    putFileToURL("http://" + catalogueAddr + ":8083/vnfpkgm/v1/vnf_packages/" + vnfInfoId + "/package_content", formData, showResultMessage, ["VNF with vnfInfoId " + vnfInfoId + " successfully updated."]);
    //putVnfPkgToURL("http://" + catalogueAddr + ":8083/vnfpkgm/v1/vnf_packages/" + vnfInfoId + "/package_content", formData, showResultMessage, ["VNF with vnfInfoId " + vnfInfoId + " successfully updated."]);
}

function getDescription(descrId) {
    var vnfId = getURLParameter('vnfId');
    console.log(vnfId);
    getVNF(vnfId, descrId, printDescription);
}

function printDescription(data, params){
    console.log(data);
    console.log('Sto facendo la print***************************************');
    var yamlObj = jsyaml.load(data);
    console.log(yamlObj);
    document.getElementById(params[1]).innerHTML = ' - ' + params[0] + ' - ' + yamlObj['description'];
}

function readVNF(graphId) {
    var vnfId = getURLParameter('vnfId');
    console.log(vnfId);
    getVNF(vnfId, graphId, createVNFGraph);
}

function getVNF(vnfInfoId, elemId, callback) {
    console.log('Sto facendo la get***************************************');
    getFileFromURL("http://" + catalogueAddr + ":8083/vnfpkgm/v1/vnf_packages/" + vnfInfoId + "/vnfd", callback, [vnfInfoId, elemId]);
    console.log('Fatta la get***************************************');
}

function showVnfGraphCanvas(data,params) {
    console.log('Guardo la canvas***************************************');
    console.log(params[0]);
    console.log(params[1]);
    console.log(data)
    console.log(data.lenght);
    for (var i in data) {
        console.log(data[i]['id']);
        if (data[i]['id'] === params[0]){
            var vnfProductName= data[i]['vnfProductName'];
            console.log(vnfProductName);
        }
        document.getElementById("graphOf_"+ data[i]['id']).style.display = "none";
    }
    //$( ".graph_canvas" ).hide();
    //$( ".graph_canvas" ).ready(function(){
        //$("#graphOf_"+ params[0]).show();
    //});
    //document.getElementByClassName("graph_canvas").style.display = "none";
    document.getElementById("graphOf_"+ params[0]).style.display = "block";
    //var cy_res='cy_'+params[0];
    var dataId ='cy_'+params[0];
    console.log("dataid="+dataId);
    document.getElementById(dataId).innerHTML = '<script>'+getVNF(params[0],dataId, createVNFGraph);+'</script>';
    //document.getElementById("graphOf_"+params[0]).innerHTML ='';
}

function createVnfInfosTable(data, params) {
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
        table.innerHTML = '<tr>No VNFs stored in Catalogue</tr>';
        return;
    }
    var btnFlag = true;
    var header = createTableHeaderByValues(['Name', 'Version', 'Provider', 'Operational State', 'Onboarding State','Actions'], btnFlag, false);
    var cbacks = ['openVNF_', 'showVnfGraphCanvas','updateVnfInfo_', 'deleteVnfInfo'];
    var names = ['View VNF', 'View VNF Graph', 'Change VNF OpState', 'Delete VNF'];
    var columns = [['vnfProductName'], ['vnfdVersion'], ['vnfProvider'], ['operationalState'], ['onboardingState']];

    table.innerHTML = header + '<tbody>';

    var rows = '';
    for (var i in data) {
        rows +=  createVnfInfosTableRow(data[i], btnFlag, cbacks, names, columns, resId);
    }
    
    table.innerHTML += rows + '</tbody>';
}

function createVnfInfosTableRow(data, btnFlag, cbacks, names, columns, resId) {
    //console.log(JSON.stringify(data, null, 4));

    var text = '';
    var btnText = '';
    if (btnFlag) {
        //btnText += createActionButton(data['id'], resId, names, cbacks);
        btnText += createLinkSet(data['id'], resId, names, cbacks);
        createUpdateVnfInfoModal(data['id'], data['operationalState'], "updateVnfInfosModals");
        creteVNFViewModal(data['id'], "vnfViewModals");
        createVnfCanvas(data);
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

function createVnfCanvas(data){
    console.log(data);
    console.log('Canvas per grafico' +  data['id']);
    var dataId ='cy_'+data['id'];
    var vnfProductName=data['vnfProductName'];
    var graphName="graphOf_" + data['id'];
    document.getElementById("vnfGraphCanvas").innerHTML += '<div id="' + graphName + '" class="graph_canvas" style="display: none";>\
                                                            <div class="col-md-12">\
                                                            <div class="x_panel">\
                                                                <div class="x_title">\
                                                                <h2>VIRTUAL NETWORK FUNCTION GRAPH - ' + vnfProductName + ' <small></small></h2>\
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


function creteVNFViewModal(vnfInfoId, modalsContainerId) {
              
    console.log('Creating view modal for vnfInfoId: ' + vnfInfoId);
    var container = document.getElementById(modalsContainerId);
    
    if (container) {
        var text = '<div id="openVNF_' + vnfInfoId + '" class="modal fade bs-example-modal-lg" tabindex="-1" role="dialog" aria-hidden="true" style="display: none;">\
                    <div class="modal-dialog modal-lg">\
                      <div class="modal-content">\
                        <div class="modal-header">\
                          <button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">×</span>\
                          </button>\
                          <h4 class="modal-title" id="myModalLabel">VNF with vnfInfoId: ' + vnfInfoId + '</h4>\
                        </div>\
                        <div class="modal-body">\
                            <textarea id="viewVNFContent_' + vnfInfoId + '" class="form-control" rows="30" readonly></textarea>\
                        </div>\
                        <div class="modal-footer">\
                          <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>\
                          <!--button type="button" class="btn btn-primary" data-dismiss="modal" onclick=updateVnfInfo("' + vnfInfoId + '","ed_' + vnfInfoId + '");>Submit</button-->\
                        </div>\
                      </div>\
                    </div>\
                </div>';
        
            container.innerHTML += text;          
            getVNF(vnfInfoId, 'viewVNFContent_' + vnfInfoId, fillVNFViewModal);
    }  
}

function fillVNFViewModal(data, params) {
    
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
    var vnfInfoId = params[0];
    var textArea = document.getElementById(params[1]);
    textArea.value = yaml;
}

function createUpdateVnfInfoModal(vnfInfoId, opState, modalsContainerId) {
    
    var container = document.getElementById(modalsContainerId);
    
    if (container) {
        var text = '<div id="updateVnfInfo_' + vnfInfoId + '" class="modal fade bs-example-modal-sm" tabindex="-1" role="dialog" aria-hidden="true" style="display: none;">\
                    <div class="modal-dialog modal-lg">\
                      <div class="modal-content">\
                        <div class="modal-header">\
                          <button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">×</span>\
                          </button>\
                          <h4 class="modal-title" id="myModalLabel">Enable / Disable VNF</h4>\
                        </div>\
                        <div class="modal-body">\
                            <form class="form-horizontal form-label-left">\
								<div class="form-group">\
									<label class="control-label col-md-3 col-sm-3 col-xs-12">Operational State</label>\
									<div class="col-md-9 col-sm-9 col-xs-12">\
										<select id="ed_' + vnfInfoId + '" class="form-control">';
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
                          <button type="button" class="btn btn-primary" data-dismiss="modal" onclick=updateVnfInfo("' + vnfInfoId + '","ed_' + vnfInfoId + '");>Submit</button>\
                        </div>\
                      </div>\
                    </div>\
                </div>';
        
        container.innerHTML += text;
    }
}

function createVNFGraph(data, params) {
    console.log('Sto facendo la graph***************************************');
    console.log(params[1]);
    var yamlObj = jsyaml.load(data);
    console.log(yamlObj);
    var nodeTempl=yamlObj['topologyTemplate']['nodeTemplates'];
    //console.log(nodeTempl);

    var nodes = [];
    var edges = [];
    var col=0;
    var ypos=50;

    $.each( nodeTempl, function(key, value){
        //console.log(key + " " + value['type']); 
        if (value['type'] === 'tosca.nodes.nfv.VNF'){
            nodes.push({group: 'nodes', data:{id: key, name: key, label: key, faveShape: 'rectangle', faveColor: '#DDDDDD'}});
            nodes.push({group: 'nodes', data:{id: 'externalCp',parent: key, name: 'externalCp', label: 'externalCp', faveShape: 'rectangle', faveColor: '#CCCCCC'}});
            $.each( nodeTempl, function(key1, value1){
                if (value1['type'] === 'tosca.nodes.nfv.Vdu.Compute'){
                    nodes.push({ group: 'nodes', data: { id: key1, col: col, parent: key, name: key1, label: key1, weight: 100, faveColor: '#DDDDDD', faveShape: 'ellipse' }, classes: 'bottom-center vnf'});
                    //edges.push({ group: 'edges', data: { source: key1, target: value1['requirements']['virtualStorage'], faveColor: '#706f6f', strength: 70 }});
                } 
            });
        }
    });
    col=col+1;
    $.each( nodeTempl, function(key, value){
            if (value['type'] === 'tosca.nodes.nfv.VnfExtCp'){
                nodes.push({ group: 'nodes', data: { id: key, col: col,parent: 'externalCp', name: key, label: key, weight: 50, faveColor: '#fff', faveShape: 'ellipse'},  classes: 'bottom-center net'});
        }
    });
    col=col+1;
    $.each( nodeTempl, function(key, value){
            if (value['type'] === 'tosca.nodes.nfv.VnfExtCp'){
            $.each( value['requirements'], function(key2, value2){
                console.log(key2 + " " + value2);
                //console.log(key);
                nodes.push({ group: 'nodes', data: { id: key2+value2, col: col, name: value2, label: value2, weight: 50, faveColor: '#fff', faveShape: 'ellipse'} , classes: 'bottom-center sap '});
                edges.push({ group: 'edges', data: { source: key, target: key2+value2, faveColor: '#706f6f', strength: 70 }});
            });
        }
    });
        /*if (value['type'] === 'tosca.nodes.nfv.VnfExtCp'){
            nodes.push({ group: 'nodes', data: { id: key, name: 'Cp - ' + key, label: 'Cp - ' + key, weight: 50, faveColor: '#fff', faveShape: 'ellipse'}, position: { x: 215, y: 85 } , classes: 'bottom-center net'});
            //console.log(value['requirements']);
            $.each( value['requirements'], function(key1, value1){
                //console.log(key1 + " " + value1);
                //console.log(key);
                nodes.push({ group: 'nodes', data: { id: key1 + value1, name: key1 + " " + value1, label: key1 + " " + value1, weight: 50, faveColor: '#fff', faveShape: 'rectangle'}, position: { x: 250, y: 85 } , classes: 'bottom-center net'});
                edges.push({ group: 'edges', data: { source: key, target: key1 + value1, faveColor: '#706f6f', strength: 70 }});
                $.each( nodeTempl, function(key2, value2){
                    if (value2['type'] === 'tosca.nodes.nfv.Vdu.Compute'){
                        edges.push({ group: 'edges', data: { source: key, target: key2, label: 'prova', faveColor: '#706f6f', strength: 70 }});
                        console.log(key + " " + key2);
                    } 
                });
            });
            //edges.push({ group: 'edges', data: { source: key, target: value1.split('/')[0], faveColor: '#706f6f', strength: 70 }});
            //value['requirements']['externalVirtualLink'];
            //nodes.push({ group: 'nodes', data: { id: value['requirements']['externalVirtualLink'], name: 'VLink - ' + value['requirements']['externalVirtualLink'], label: 'VLink - ' + value['requirements']['externalVirtualLink'], weight: 50, faveColor: '#fff', faveShape: 'ellipse' }, classes: 'bottom-center faded'});
        } else if (value['type'] === 'tosca.nodes.nfv.Vdu.Compute'){
            nodes.push({ group: 'nodes', data: { id: key, name: 'NODE - ' + key, label: 'NODE - ' + key, weight: 70, faveColor: '#fff', faveShape: 'ellipse' }, position: { x: 300, y: 85 }, classes: 'bottom-center vnf'}); 
        }
        //console.log(nodes);
    });
    */

    var cy = cytoscape({
        container: document.getElementById(params[1]),

        boxSelectionEnabled: false,
        autounselectify: true,

		layout: {
			name: 'grid',
            padding: 1,
            position: function(node) { 
                return {
                 row: node.data('row'),
                 col: node.data('col')
                } 
              }
		},

		style: cytoscape.stylesheet()
			.selector('node')
				.css({
                    'shape': 'data(faveShape)',
                    'height': 80,
                    'width': 80,
                    'content': 'data(name)',
					'text-valign': 'center',
					'text-outline-width': 0,
					'text-width': 2,
					//'text-outline-color': '#000',
					'background-color': 'data(faveColor)',
					'color': '#000',
					'label': 'data(name)'
                })
            .selector('$node > node')
                .css({
                    'shape': 'data(faveShape)',
                    'color': '#000',
                    'text-pt': 10,
                    'text-outline-width': 0,
                    //'text-outline-color': '#000',
                    'padding-top': '10px',
                    'padding-left': '30px',
                    'padding-bottom': '10px',
                    'padding-right': '30px',
                    'text-valign': 'top',
                    'text-halign': 'center',
                    'background-color': 'data(faveColor)'
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
                    'label': 'data(label)',
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
					'background-image': '/5gcatalogue/images/vdu.png',
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
					'background-image': '/5gcatalogue/images/cp.png',
					'width': 50,//'mapData(weight, 40, 80, 20, 60)',
					'height': 50
				})
			.selector('.sap')
				.css({
					'background-image': '/5gcatalogue/images/net.png',
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
