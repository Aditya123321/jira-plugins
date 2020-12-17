AJS.$(window).load(function() {
    //disable next button on load
    $("#nextButton").prop("disabled", true);

    //check if file is changed
    $("#uniqueInputFile").on("change", function(e) {
        try {
            closeMessages();
            if (e.target.files.length > 0) {
                var fileName = e.target.files[0].name;
                var fileSize = e.target.files[0].size;
                if (!validateFile(fileSize, fileName)) {
                    try {
                        $("#nextButton").prop("disabled", true);
                        $("#fileLabel").removeAttr("data-ffi-value");
                        $("#ffi-clear").css("visibility", "hidden");
                        $("#uniqueInputFile").val("");
                        //return false;
                    } catch (error) {
                        console.log(error);
                    }
                    return false;
                }
                if (fileName !== "") {
                    $("#ffi-clear").css("visibility", "visible");
                    $("#fileLabel").attr("data-ffi-value", fileName);
                }
            } else {
                $("#nextButton").prop("disabled", true);
                $("#fileLabel").removeAttr("data-ffi-value");
                $("#ffi-clear").css("visibility", "hidden");
            }
        } catch (error) {
            console.log(error);
        }
    });

    //clear file input
    $("#ffi-clear").on("click", function(e) {
        try {
            $("#nextButton").prop("disabled", true);
            $("#fileLabel").removeAttr("data-ffi-value");
            $(this).css("visibility", "hidden");
            $("#uniqueInputFile").val("");
            closeMessages();
        } catch (error) {
            console.log(error);
        }
    });

    //on click of upload button
    AJS.$("#uploadUniqueFile").click(function(event) {
        event.preventDefault();
        try {
            storeCSVFile();
        } catch (error) {
            console.log(error);
        }
        return false;
    });

    //message close button
    AJS.$(".aui-iconfont-cross-close").click(function(event) {
        try {
            $("#fileUploadStatusMessage").css("visibility", "hidden");
        } catch (error) {
            console.log(error);
        }
    });
});

//send file
function storeCSVFile() {
    var fd = new FormData();
    var files = AJS.$("#uniqueInputFile")[0].files[0];
    if (AJS.$("#uniqueInputFile").val() == "") {
        showMessage("Please select CSV file first!");
        handleFileUploadResponse("Please select CSV file first!", "warning");
        $("#nextButton").prop("disabled", false);
        console.log("warning", d)
        return false;
    }
    $("#fileLoadingAnimation").css("visibility", "visible");
    fd.append("file", files);
    fd.append("projectName", "SAMPROJ");

    var hostExtension = "";
    if (location.hostname === "localhost" || location.hostname === "127.0.0.1")
        hostExtension = "/jira";
    // send file and project name to backend
    AJS.$.ajax({
        url: hostExtension + "/plugins/servlet/taestimateupload",
        type: "POST",
        enctype: "multipart/form-data",
        data: fd,
        contentType: false,
        processData: false,
        success: function(d) {
            handleFileUploadResponse("Upload successful!", "success");
            $("#nextButton").prop("disabled", false);
            console.log("Success", d);
        },
        error: function(error) {
            handleFileUploadResponse("Sorry, Upload failed!", "error");
            $("#nextButton").prop("disabled", true);
            console.log("error", error);
        },
    });
    return false;
}

//show status on file upload
function handleFileUploadResponse(message, status) {
    $("#fileLoadingAnimation").css("visibility", "hidden");
    try {
        var classList = $("#fileUploadStatusMessage").attr("class").split(/\s+/);
        for (var i = 0; i < classList.length; i++) {
            var myclass = "";
            if (classList[i] !== " ") {
                if (
                    classList[i].includes("aui-message-") &&
                    !classList[i].includes("-controls")
                ) {
                    $("#fileUploadStatusMessage").removeClass(classList[i]);
                    $("#fileUploadStatusMessage").addClass("aui-message-" + status);
                    showMessage(message);
                    return false;
                }
            }
        }
    } catch (error) {
        console.log(error);
    }
}

//close status messages
function closeMessages() {
    $("#fileUploadStatusMessage").css("visibility", "hidden");
}

//function validate file type
function validateFile(fileSize, fileName) {
    const FILE_SIZE = 1048576;
    const FILE_NAME_LIMIT = 15;
    var validExtensions = ["csv", "CSV"];
    if (fileSize > FILE_SIZE) {
        handleFileUploadResponse("File size too big!", "warning");
        return false;
    }
    if (fileName === undefined || fileName.length > FILE_NAME_LIMIT) {
        handleFileUploadResponse("File name is too long!", "warning");
        return false;
    }
    try {
        var fileNameExt = fileName.substr(fileName.lastIndexOf(".") + 1);
        if ($.inArray(fileNameExt, validExtensions) == -1) {
            handleFileUploadResponse("Please select CSV file only!", "warning");
            return false;
        }
    } catch (error) {
        console.log(error);
    }
    return true;
}

//display message
function showMessage(text) {
    $("#showStatusMessage").text(text);
    $("#fileUploadStatusMessage").css("visibility", "visible");
}