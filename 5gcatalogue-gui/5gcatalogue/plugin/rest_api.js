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

function checkUser(cname) {
    var cvalue = getCookie(cname);
    
    if (cvalue == '') {
        return false;
    }
    
    return true;
}

function loginToURL(resourceUrl, username, password, callback, failedCallback) {
    var settings = {
        "async": true,
        "crossDomain": true,
        "url": resourceUrl,
        "method": "POST",
        "headers": {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        xhrFields: {
            withCredentials: true
        },
        "data": {
            "username": username,
            "password": password
        }
    };
      
    $.ajax(settings).done(function (response) {
        console.log(response);
        callback();
    }).fail(failedCallback ? failedCallback : function() {});
}

function getJsonFromURL(resourceUrl, callback, params) {
    var settings = {
        "async": true,
        //"crossDomain": true,
        "url": resourceUrl,
        "method": "GET",
        headers: {
            Accept: "application/json; charset=utf-8"
        }
    };

    $.ajax(settings).done(function (response) {
        //console.log(response);
        callback(response, params);
    }).fail(function (response) {
        console.log(response);
    });
}

function getJsonFromURLWithAuth(resourceUrl, callback, params) {
    var settings = {
        "async": true,
        //"crossDomain": true,
        "url": resourceUrl,
        "method": "GET",
        headers: {
            Accept: "application/json; charset=utf-8",
            Authorization: "Bearer " + getCookie("TOKEN")
        }
    };

    $.ajax(settings).done(function (response) {
        //console.log(response);
        callback(response, params);
    }).fail(function (response) {
        console.log(response);
        if (response.status == 401) {
            location.href = '/5gcatalogue/401.html';
        } else if (response.status == 403) {
            location.href = '/5gcatalogue/403.html';
        }
    });
}


function getFileFromURL(resourceUrl, callback, params) {
    var settings = {
        "async": true,
        //"crossDomain": true,
        "url": resourceUrl,
        "method": "GET",
        headers: {
            Accept: "application/yaml; charset=utf-8"
        }
    };

    $.ajax(settings).done(function (response) {
        //console.log(response);
        callback(response, params);
    }).fail(function (response) {
        console.log(response);
    });
}

function getFileFromURLWithAuth(resourceUrl, callback, params) {
    if (!checkUser('username')) {
        redirectToError('401');
    } else {
        var settings = {
            "async": true,
            //"crossDomain": true,
            "url": resourceUrl,
            "method": "GET",
            headers: {
                Accept: "application/yaml; charset=utf-8"
            },
            xhrFields: {
                withCredentials: true
            }
        };

        $.ajax(settings).done(function (response) {
            //console.log(response);
            callback(response, params);
        }).fail(function (response) {
            console.log(response);
            if (response.status == 401) {
                location.href = '/5gcatalogue/401.html';
            } else if (response.status == 403) {
                location.href = '/5gcatalogue/403.html';
            }
        });
    }
}

function getFromURLWithAuth(resourceUrl, callback, params) {
    if (!checkUser('username')) {
        redirectToError('401');
    } else {
        var settings = {
            "async": true,
            "crossDomain": true,
            "url": resourceUrl,
            "method": "GET",
            xhrFields: {
                withCredentials: true
            }
        };

        $.ajax(settings).done(function () {
            callback(params);
        }).fail(function (response) {
            console.log(response);
            if (response.status == 401) {
                location.href = '/5gcatalogue/401.html';
            } else if (response.status == 403) {
                location.href = '/5gcatalogue/403.html';
            }
        });
    }
}

function postJsonToURL(resourceUrl, jsonData, callback, params) {
    var settings = {
        "async": true,
        //"crossDomain": true,
        "url": resourceUrl,
        "method": "POST",
        "headers": {
            Accept: "application/json, application/yaml",
            "Content-Type": "application/json"
        },
        "data": jsonData
    };

    $.ajax(settings).done(function (response) {
        console.log(response);
        if (callback == showResultMessage) {
            callback(true, params[0]);
        } else {
            callback(response, params);
        }
    }).fail(function (response) {
        console.log(response);
        if (callback == showResultMessage) {
            var errorMsg = "Status code: " + response.responseJSON.status;
            errorMsg +=  " Reason: " + response.responseJSON.detail;
            callback(false, errorMsg);
        }
    });
}

function postJsonToURLWithAuth(resourceUrl, jsonData, callback, params) {
    if (!checkUser('username')) {
        redirectToError('401');
    } else {
        var settings = {
            "async": true,
            //"crossDomain": true,
            "url": resourceUrl,
            "method": "POST",
            "headers": {
                Accept: "application/json, application/yaml",
                "Content-Type": "application/json"
            },
            "data": jsonData,
            xhrFields: {
                withCredentials: true
            }
        };

        $.ajax(settings).done(function (response) {
            console.log(response);
            if (callback == showResultMessage) {
                callback(true, params[0]);
            } else {
                callback(response, params);
            }
        }).fail(function (response) {
            console.log(response);
            if (response.status == 401) {
                location.href = '/5gcatalogue/401.html';
            } else if (response.status == 403) {
                location.href = '/5gcatalogue/403.html';
            } else if (callback == showResultMessage) {
                var errorMsg = "Status code: " + response.responseJSON.status;
                errorMsg +=  " Reason: " + response.responseJSON.detail;
                callback(false, errorMsg);
            }
        });
    }
}

function postToURL(resourceUrl, callback, params) {
    var settings = {
        "async": true,
        "crossDomain": true,
        "url": resourceUrl,
        "method": "POST",
    };

    $.ajax(settings).done(function (response) {
        console.log(response);
        if (callback == showResultMessage) {
            callback(true, params[0]);
        } else {
            callback(response, params);
        }
    }).fail(function (response) {
        console.log(response);
        if (callback == showResultMessage) {
            var errorMsg = "Status code: " + response.responseJSON.status;
            errorMsg +=  " Reason: " + response.responseJSON.detail;
            callback(false, errorMsg);
        }
    });
}

function postToURLWithAuth(resourceUrl, callback, params) {
    if (!checkUser('username')) {
        redirectToError('401');
    } else {
        var settings = {
            "async": true,
            "crossDomain": true,
            "url": resourceUrl,
            "method": "POST",
            xhrFields: {
                withCredentials: true
            }
        };

        $.ajax(settings).done(function (response) {
            console.log(response);
            if (callback == showResultMessage) {
                callback(true, params[0]);
            } else {
                callback(response, params);
            }
        }).fail(function (response) {
            console.log(response);
            if (response.status == 401) {
                location.href = '/5gcatalogue/401.html';
            } else if (response.status == 403) {
                location.href = '/5gcatalogue/403.html';
            } else if (callback == showResultMessage) {
                var errorMsg = "Status code: " + response.responseJSON.status;
                errorMsg +=  " Reason: " + response.responseJSON.detail;
                callback(false, errorMsg);
            }
        });
    }
}

function putJsonToURLWithAuth(resourceUrl, jsonData, callback, params) {
    if (!checkUser('username')) {
        redirectToError('401');
    } else {
        var settings = {
            "async": true,
            "crossDomain": true,
            "url": resourceUrl,
            "method": "PUT",
            "headers": {
                "Content-Type": "application/json"
            },
            xhrFields: {
                withCredentials: true
            },
            "data": jsonData
        };

        $.ajax(settings).done(function (response) {
            console.log(response);
            callback(response, params);
        }).fail(function (response) {
            console.log(response);
            if (response.status == 401) {
                location.href = '/5gcatalogue/401.html';
            } else if (response.status == 403) {
                location.href = '/5gcatalogue/403.html';
            }
        });
    }
}

function putFileToURL(resourceUrl, file, callback, params) {
    var settings = {
        "async": true,
        //"crossDomain": true,
        "url": resourceUrl,
        "method": "PUT",
        data: file,
        cache: false,
        "contentType": false,
        processData: false
    };

    $.ajax(settings).done(function (response) {
        console.log(response);
        if (callback == showResultMessage) {
            callback(true, params[0]);
        } else {
            callback(response, params);
        }
    }).fail(function (response) {
        console.log(response);
        if (callback == showResultMessage) {
            var errorMsg = "Status code: " + response.responseJSON.status;
            errorMsg +=  " Reason: " + response.responseJSON.detail;
            callback(false, errorMsg);
        }
    });
}

function putFileToURLWithAuth(resourceUrl, file, callback, params) {
    if (!checkUser('username')) {
        redirectToError('401');
    } else {
        var settings = {
            "async": true,
            //"crossDomain": true,
            "url": resourceUrl,
            "method": "PUT",
            data: file,
            cache: false,
            "contentType": false,
            processData: false,
            xhrFields: {
                withCredentials: true
            }
        };

        $.ajax(settings).done(function (response) {
            console.log(response);
            if (callback == showResultMessage) {
                callback(true, params[0]);
            } else {
                callback(response, params);
            }
        }).fail(function (response) {
            console.log(response);
            if (response.status == 401) {
                location.href = '/5gcatalogue/401.html';
            } else if (response.status == 403) {
                location.href = '/5gcatalogue/403.html';
            } else if (callback == showResultMessage) {
                var errorMsg = "Status code: " + response.responseJSON.status;
                errorMsg +=  " Reason: " + response.responseJSON.detail;
                callback(false, errorMsg);
            }
        });
    }
}

function putToURLWithAuth(resourceUrl, callback, params) {
    if (!checkUser('username')) {
        redirectToError('401');
    } else {
        var settings = {
            "async": true,
            "crossDomain": true,
            "url": resourceUrl,
            "method": "PUT",
            xhrFields: {
                withCredentials: true
            }
        };

        $.ajax(settings).done(function (response) {
            console.log(response);
            callback(response);
        }).fail(function (response) {
            console.log(response);
            if (response.status == 401) {
                location.href = '/5gcatalogue/401.html';
            } else if (response.status == 403) {
                location.href = '/5gcatalogue/403.html';
            }
        });
    }
}

function deleteRequestToURL(resourceUrl, callback, params) {
    var settings = {
        "async": true,
        //"crossDomain": true,
        "url": resourceUrl,
        "method": "DELETE",
    };

    $.ajax(settings).done(function (response) {
        console.log(response);
        if (callback == showResultMessage) {
            callback(true, params[0]);
        } else {
            callback(response, params);
        }
    }).fail(function (response) {
        console.log(response);
        if (callback == showResultMessage) {
            var errorMsg = "Status code: " + response.responseJSON.status;
            errorMsg +=  " Reason: " + response.responseJSON.detail;
            callback(false, errorMsg);
        }
    });
}

function deleteRequestToURLWithAuth(resourceUrl, callback, params) {
    if (!checkUser('username')) {
        redirectToError('401');
    } else {
        var settings = {
            "async": true,
            //"crossDomain": true,
            "url": resourceUrl,
            "method": "DELETE",
            xhrFields: {
                withCredentials: true
            }
        };

        $.ajax(settings).done(function (response) {
            console.log(response);
            if (callback == showResultMessage) {
                callback(true, params[0]);
            } else {
                callback(response, params);
            }
        }).fail(function (response) {
            console.log(response);
            if (response.status == 401) {
                location.href = '/5gcatalogue/401.html';
            } else if (response.status == 403) {
                location.href = '/5gcatalogue/403.html';
            } else if (callback == showResultMessage) {
                var errorMsg = "Status code: " + response.responseJSON.status;
                errorMsg +=  " Reason: " + response.responseJSON.detail;
                callback(false, errorMsg);
            }
        });
    }
}

function patchJsonRequestToURL(resourceUrl, jsonData, callback, params) {
    var settings = {
        "async": true,
        //"crossDomain": true,
        "url": resourceUrl,
        "method": "PATCH",
        "headers": {
            Accept: "application/json; charset=utf-8",
            "Content-Type": "application/json"
        },
        "data": jsonData
    };

    $.ajax(settings).done(function (response) {
        console.log(response);
        if (callback == showResultMessage) {
            callback(true, params[0]);
        } else {
            callback(response, params);
        }
    }).fail(function (response) {
        console.log(response);
        if (callback == showResultMessage) {
            var errorMsg = "Status code: " + response.responseJSON.status;
            errorMsg +=  " Reason: " + response.responseJSON.detail;
            callback(false, errorMsg);
        }
    });
}

function patchJsonRequestToURLWithAuth(resourceUrl, jsonData, callback, params) {
    if (!checkUser('username')) {
        redirectToError('401');
    } else {
        var settings = {
            "async": true,
            //"crossDomain": true,
            "url": resourceUrl,
            "method": "PATCH",
            "headers": {
                Accept: "application/json; charset=utf-8",
                "Content-Type": "application/json"
            },
            "data": jsonData,
            xhrFields: {
                withCredentials: true
            }
        };

        $.ajax(settings).done(function (response) {
            console.log(response);
            if (callback == showResultMessage) {
                callback(true, params[0]);
            } else {
                callback(response, params);
            }
        }).fail(function (response) {
            console.log(response);
            if (response.status == 401) {
                location.href = '/5gcatalogue/401.html';
            } else if (response.status == 403) {
                location.href = '/5gcatalogue/403.html';
            } else if (callback == showResultMessage) {
                var errorMsg = "Status code: " + response.responseJSON.status;
                errorMsg +=  " Reason: " + response.responseJSON.detail;
                callback(false, errorMsg);
            }
        });
    }
}