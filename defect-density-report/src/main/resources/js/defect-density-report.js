AJS.$(document).ready(function() {
    $(".aui-message.aui-message-info.info .excel a").text("Download Excel");
    //Prepopulation of calender filters
    var fromDate = $('#from-date-hidden').val();
    $('#from-date').val(fromDate);

    var toDate = $('#to-date-hidden').val();
    $('#to-date').val(toDate);

    //Sorting for defect density reports
    $('.defect-report-content thead th').click(function(){
        var table = $(this).parents('table').eq(0);
        var rows = table.find('tbody tr').toArray().sort(comparer($(this).index()));
        var downArrow = $(this).find('.down-arrow');
        var upArrow = $(this).find('.up-arrow');

        this.asc = !this.asc;
        $('.arrow').addClass('hidden');

        if (!this.asc) {
            upArrow.removeClass('hidden');
        } else {
            downArrow.removeClass('hidden');
            rows = rows.reverse();
        }
        for (var i = 0; i < rows.length; i++)
        {
            table.append(rows[i]);
        }
    })

    function comparer(index) {
        return function(a, b) {
            var valA = getCellValue(a, index),
                valB = getCellValue(b, index);
            return $.isNumeric(valA) && $.isNumeric(valB) ? valA - valB : valA.toString().localeCompare(valB);
        }
    }

    function getCellValue(row, index) { 
        return $(row).children('td').eq(index).text(); 
    }

    //Sorting arrows
    var arrowDiv = '<span class="arrow up-arrow">&#8593;</span>'+
                   '<span class="arrow down-arrow">&#8595;</span>';
    $('.defect-report-content thead th').append(arrowDiv);

    // Tooltip for table headers
    $('.defect-report-content thead th').hover(function() {
        $(this).children('.header-tooltip').removeClass('hidden');
        
    }, 
    function () {
        $(this).children('.header-tooltip').addClass('hidden');
    })
});