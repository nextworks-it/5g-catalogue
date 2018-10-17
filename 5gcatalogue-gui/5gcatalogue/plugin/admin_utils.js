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
	getJsonFromURL("http://" + catalogueAddr + ":8083/catalogue/manoManagement/manos", createPluginsTable, [tableId, resId]);
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