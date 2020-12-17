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

    // Multiple select filter in project-estimates-report --------- START
    // Multiple select filter dropdown
    $("#select-status").on("click", function(e){
        $("#checkboxes").show();
         e.stopPropagation();
    });

    $("#checkboxes").on("click", function(e){
        e.stopPropagation();
    });
    
    $(document).on("click", function(){
        $("#checkboxes").hide();
    });

    // Select all functionality
    $("#select-all").on("click", function() {
        var checkbox = $(".status-list input:checkbox");
        if ($("#select-all").prop("checked")) {
            checkbox.attr("checked", true);
        }
        else {
            checkbox.attr("checked", false);
        } 
    })

    // Multiple select filter prepopulation
    function prepopulateStatus() {
        var statusAvail = $(".status-list input");
        var statusUrl = $(".hidden-status");
        var checkbox = $(".status-list input:checkbox").get();
        
        var checkboxes = $(".status-list [name=status]");
        var allStatusChecked = true;

        for(var i=0; i< statusAvail.length; i++) {
            for(var j=0; j<statusUrl.length; j++) {
                if(statusAvail[i].value.toUpperCase() === statusUrl[j].value.toUpperCase()) {
                    $(checkbox[i]).attr("checked", true);
                }
            }
        }

        for(i=0; i< checkboxes.length; i++) {
            if(!$(checkboxes[i]).prop("checked")) {
                allStatusChecked = false;
                break;
            } 
        }
        
        if(allStatusChecked && !($("#select-all").prop("checked"))) {
            $("#select-all").attr("checked", true);
        }
    }
    prepopulateStatus();
    // Multiple select filter in project estimates report --------- END

    // Prepopulation of calender filters
    var fromDate = $('#from-date-hidden').val();
    $('#from-date').val(fromDate);
 
    var toDate = $('#to-date-hidden').val();
    $('#to-date').val(toDate);

    
    //Sorting for project estimates reports --------- START
    var flagForSubStoriesSorting = false;
    var tableOnLoad = $('.timeReport thead th').parents('table').eq(0);
    var subData = [];
    var tempRows = tableOnLoad.find('tbody tr');
    // 'tag' is for story, 'subTaskArray' is for substories
    var tempTag = {
        tag: '',
        subTaskArr: []
    };     

    for(var i = 0; i<tempRows.length; i++) { 
        if($(tempRows[i]).hasClass('aui-expander-subtask')){
            tempTag.subTaskArr.push(tempRows[i]);
            if(((i+1) < tempRows.length && !$(tempRows[i+1]).hasClass('aui-expander-subtask')) || i === (tempRows.length-1)) {
                subData.push(tempTag);
            }
        } else {
            tempTag = { tag: tempRows[i], subTaskArr: []};
            if((i === tempRows.length-1) || ((i+1) < tempRows.length && !$(tempRows[i+1]).hasClass('aui-expander-subtask'))) {
                subData.push(tempTag);
            }
        }
    }

    $('.timeReport thead th').click(function(){
        var table = $(this).parents('table').eq(0);
        var downArrow = $(this).find('.down-arrow');
        var upArrow = $(this).find('.up-arrow');
        var currentIndex = $(this).index();
        var rows = table.find('tbody tr').toArray().sort(comparer(currentIndex));
        this.asc = !this.asc;

        if($(this).data('id') === 'time-estimates-report') {
            subData = subData.sort(function(a, b) {
                var valA = $(a.tag).children('td').eq(currentIndex)[0].innerText,
                    valB = $(b.tag).children('td').eq(currentIndex)[0].innerText;
                var numA = parseFloat(valA),
                    numB = parseFloat(valB);
                    console.log(valA, valB, numA, numB);
                if(valA.includes('-') && valB.includes('-')) {
                    return valA.split('-')[1] - valB.split('-')[1];
                } else if(!isNaN(numA) && !isNaN(numB)) {
                    return numA - numB;
                } else {
                    return valA.toString().localeCompare(valB);
                }
                    
            });
        }
        // currentIndex === 0 is for Story Id column

        $('.arrow').addClass('hidden');

        if (!this.asc) {
            upArrow.removeClass('hidden');
        } else {
            downArrow.removeClass('hidden');
            rows = rows.reverse();
            subData = subData.reverse();
        }
        if(!flagForSubStoriesSorting) {
            flagForSubStoriesSorting = true;
        } else {
            for(i=0; i<subData.length; i++) {
                if(currentIndex === 0) {
                    subData[i].subTaskArr = subData[i].subTaskArr.reverse();
                }
            }
        }
        
        if($(this).data('id') === 'time-estimates-report') {
            for(i=0; i<subData.length; i++) {
                var tasks = subData[i];
                table.append(tasks.tag);
                for(var j=0; j<tasks.subTaskArr.length; j++) {
                    var subTask = tasks.subTaskArr[j];
                    table.append(subTask);
                }
            }
        }
        else {
            for (i = 0; i < rows.length; i++)
            {
                table.append(rows[i]);
            }
        }
        
    });

    function comparer(index) {
        return function(a, b) {
            var valA = getCellValue(a, index),
                valB = getCellValue(b, index);
            return $.isNumeric(valA) && $.isNumeric(valB) ? valA -  valB : valA.toString().localeCompare(valB);
        }
    }

    function getCellValue(row, index) { 
        return $(row).children('td').eq(index).text(); 
    }

    //Sorting arrows
    var arrowDiv = '<span class="arrow up-arrow">&#8593;</span>'+
                   '<span class="arrow down-arrow">&#8595;</span>';
    $('.timeReport thead th').append(arrowDiv);
    //Sorting for project estimates reports --------- END

    // Tooltip for table headers
    $('.timeReport thead th').hover(function() {
        $(this).children('.header-tooltip').removeClass('hidden');
    }, 
    function () {
        $(this).children('.header-tooltip').addClass('hidden');
    })
});