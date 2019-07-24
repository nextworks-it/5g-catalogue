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

function getAllMANOPlugins(tableId, resId) {
	getJsonFromURLWithAuth("http://" + catalogueAddr + ":8083/catalogue/manoManagement/manos", createPluginsTable, [tableId, resId]);
}

function getAllProjects(tableId, resId, callback) {
	getJsonFromURLWithAuth("http://" + catalogueAddr + ":8083/catalogue/projectManagement/projects", callback, [tableId, resId]);
}

function getAllUsers(tableId, resId) {
	getJsonFromURLWithAuth("http://" + catalogueAddr + ":8083/catalogue/userManagement/users", createUsersTable, [tableId, resId]);
}

function getUser(divId, resId, callback) {
    getJsonFromURLWithAuth("http://" + catalogueAddr + ":8083/catalogue/userManagement/users/" + divId, callback, [divId, resId]);
}

function createNewProject(inputs) {
    var projectId = document.getElementById(inputs[0]).value;
    var projectDescription = document.getElementById(inputs[1]).value;

    var jsonObj = JSON.parse('{}');
    jsonObj['projectId'] = projectId;
    jsonObj['projectDescription'] = projectDescription;

    var json = JSON.stringify(jsonObj, null, 4);

    postProject(projectId, json);
}

function createNewUser(inputs) {
    var userName = document.getElementById(inputs[0]).value;
    var firstName = document.getElementById(inputs[1]).value;
    var lastName = document.getElementById(inputs[2]).value;
    var defaultProj = document.getElementById(inputs[3]).value;

    var jsonObj = JSON.parse('{}');
    if (userName)
        jsonObj['userName'] = userName.toLowerCase();
    console.log(userName);
    if (firstName)
        jsonObj['firstName'] = firstName;
    if (lastName)
        jsonObj['lastName'] = lastName;
    if (defaultProj)
        jsonObj['defaultProject'] = defaultProj;

    var json = JSON.stringify(jsonObj, null, 4);

    console.log("New User: " + json);

    postUser(userName, json);
}

function putUserToProject(userNameInput, projectIdInput) {
    var userName = document.getElementById(userNameInput).value;
    var projecId = document.getElementById(projectIdInput).value;
    putToURLWithAuth("http://" + catalogueAddr + ":8083/catalogue/userManagement/projects/" + projecId + "/users/" + userName, showResultMessage, ["User" + userName + " has been successfully added to Project " + projecId + "."]);
}

function postProject(projectId, data) {
    postJsonToURLWithAuth("http://" + catalogueAddr + ":8083/catalogue/projectManagement/projects", data, showResultMessage, ["Project " + projectId + " has been successfully created."]);
}

function postUser(userName, data) {
    postJsonToURLWithAuth("http://" + catalogueAddr + ":8083/catalogue/userManagement/users", data, showResultMessage, ["User " + userName + " has been successfully created."]);
}

function createPluginsTable(data, params) {
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
        table.innerHTML = '<tr>No Plugins instantiated in Catalogue</tr>';
        return;
    }
    var btnFlag = false;
    var header = createTableHeaderByValues(['Id', 'Type', 'IP Address'], btnFlag, false);
    var cbacks = [];
    var names = [];
    var columns = [['manoId'], ['manoType'], ['ipAddress']];

    table.innerHTML = header + '<tbody>';

    var rows = '';
    for (var i in data) {
        rows +=  createPluginsTableRow(data[i], btnFlag, cbacks, names, columns, resId);
    }
    
    table.innerHTML += rows + '</tbody>';
}

function createPluginsTableRow(data, btnFlag, cbacks, names, columns, resId) {
    //console.log(JSON.stringify(data, null, 4));

    var text = '';
    var btnText = '';
    if (btnFlag) {
        btnText += createActionButton(data['manoId'], resId, names, cbacks);
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

function createProjectsTable(data, params) {
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
        table.innerHTML = '<tr>No Projects created in Catalogue</tr>';
        return;
    }
    var btnFlag = false;
    var header = createTableHeaderByValues(['Id', 'Description', 'Users'], btnFlag, false);
    var cbacks = [];
    var names = [];
    var columns = [['projectId'], ['projectDescription'], ['users']];

    table.innerHTML = header + '<tbody>';

    var rows = '';
    for (var i in data) {
        rows +=  createProjectsTableRow(data[i], btnFlag, cbacks, names, columns, resId);
    }
    
    table.innerHTML += rows + '</tbody>';
}

function createProjectsTableRow(data, btnFlag, cbacks, names, columns, resId) {
    //console.log(JSON.stringify(data, null, 4));

    var text = '';
    var btnText = '';
    if (btnFlag) {
        btnText += createActionButton(data['projectId'], resId, names, cbacks);
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
            } else if (values[0] instanceof Array && values[0].length >= 1) {
                for (var vv in values[0]) {
                    subTable += '<tr><td>' + values[0][vv] + '</td><tr>';
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

function createUsersTable(data, params) {
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
        table.innerHTML = '<tr>No Users in Catalogue</tr>';
        return;
    }
    var btnFlag = true;
    var header = createTableHeaderByValues(['Username', 'First Name', 'Last name', 'Default Project'], btnFlag, false);
    var cbacks = ['addUserToProject_'];
    var names = ['Add to Project'];
    var columns = [['userName'], ['firstName'], ['lastName'], ['defaultProject']];

    table.innerHTML = header + '<tbody>';

    var rows = '';
    for (var i in data) {
        rows +=  createUsersTableRow(data[i], btnFlag, cbacks, names, columns, resId);
    }
    
    table.innerHTML += rows + '</tbody>';
    getJsonFromURLWithAuth("http://" + catalogueAddr + ":8083/catalogue/projectManagement/projects", fillCreateUserModal, ["defaultProject", "response"]);
}

function createUsersTableRow(data, btnFlag, cbacks, names, columns, resId) {
    //console.log(JSON.stringify(data, null, 4));

    var text = '';
    var btnText = '';
    if (btnFlag) {
        btnText += createLinkSet(data['userName'], resId, names, cbacks);

        getJsonFromURLWithAuth("http://" + catalogueAddr + ":8083/catalogue/projectManagement/projects", createAddToProjectModal, [data['userName'], "addToProjectModals", "response"]);
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

function fillCreateUserModal(data, params) {
    var select = document.getElementById(params[0]);

    for (var i in data) {
        select.innerHTML += '<option value=' + data[i]['projectId'] + '>' + data[i]['projectId'] + '</option>';
    }
}

function createAddToProjectModal(data, params) {
    var container = document.getElementById(params[1]);

    if (container) {
        var text = '<div id="addUserToProject_' + params[0] + '" class="modal fade bs-example-modal-sm" tabindex="-1" role="dialog" aria-hidden="true" style="display: none;">\
                    <div class="modal-dialog modal-lg">\
                      <div class="modal-content">\
                        <div class="modal-header">\
                          <button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">×</span>\
                          </button>\
                          <h4 class="modal-title" id="myModalLabel">Add User to Project</h4>\
                        </div>\
                        <div class="modal-body">\
                            <form class="form-horizontal form-label-left">\
                                <div class="form-group">\
                                    <label class="control-label col-md-3 col-sm-3 col-xs-12" for="first-name">Username <span class="required">*</span>\
                                    </label>\
                                    <div class="col-md-6 col-sm-6 col-xs-12">\
                                        <input type="text" value="' + params[0] + '" id="userName_' + params[0] + '" required="required" class="form-control col-md-7 col-xs-12" disabled>\
                                    </div>\
                                </div>\
								<div class="form-group">\
									<label class="control-label col-md-3 col-sm-3 col-xs-12">Project </label>\
									<div class="col-md-6 col-sm-6 col-xs-12">\
										<select id="projectId_' + params[0] + '" class="form-control">';
        for (var i in data) {
            text += '<option value=' + data[i]['projectId'] + '>' + data[i]['projectId'] + '</option>';
        }
		text += '</select>\
									</div>\
								</div>\
							</form>\
                        </div>\
                        <div class="modal-footer">\
                          <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>\
                          <button type="button" class="btn btn-primary" data-dismiss="modal" onclick=putUserToProject("userName_' + params[0] + '","projectId_' + params[0] + '");>Submit</button>\
                        </div>\
                      </div>\
                    </div>\
                </div>';

        container.innerHTML += text;
    }
}

function fillProjectsData(data, params) {
    var projCookie = getCookie("PROJECT");
    //console.log("Current project: " + projCookie);

    var service_role = getCookie("ROLE");
    //console.log("Current service role: " + service_role);
    if(service_role.indexOf("ROLE_SERVICE_ADMIN") >= 0) {
        if (projCookie == null) {
            setCookie("PROJECT", "admin", 1);
            projCookie = "admin";
        }
    } else {
        if (projCookie == null) {
            if (data['projects'].length >= 1) {
                setCookie("PROJECT", data['projects'][0], 1);
                projCookie = data['projects'][0];
            }
        }
    }

    document.getElementById('project').innerHTML = projCookie;
    document.getElementById('projectBar').innerHTML = '<b>' + projCookie + '</b>';
    
    var projectsDropDown = document.getElementById("userProjects");
    
    for (var i = 0 ; i < data['projects'].length; i++) {
        console.log("Project #" + i + ": "  + data['projects'][i]);
        projectsDropDown.innerHTML += '<li><a onclick=selectProject("' + data['projects'][i] + '"); href="#">' + data['projects'][i] + '</a></li>';
    }    
}