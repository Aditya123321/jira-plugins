AJS.$(document).ready(function() {
    AJS.$(".aui-button-subtask").on("click", function(event) {
        var classes = AJS.$(this).attr("class");
        var classArray = classes.split(" ");
        var lastClass = "";
        for (var i = 0; i < classArray.length; i++) {
            if (classArray[i].includes("aui-button-subtask-")) {
                lastClass = classArray[i];
                break;
            }
        }
        var rowArr = lastClass.split("-");
        var rowId = rowArr[rowArr.length - 1];
        var targetRowClass = ".aui-row-subtask-" + rowId;

        var targetRow = AJS.$(targetRowClass);

        var downArrow = "aui-subtask-icon-click";
        var upArrow = "aui-subtask-icon";
        AJS.$(this).toggleClass(downArrow);
        AJS.$(this).toggleClass(upArrow);
        targetRow.toggleClass("aui-expander-subtask-visible");
    });
});