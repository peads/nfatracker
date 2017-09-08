/**
 * Created by peads on 9/6/17.
 */
$(function () {
    $('.ui-datepicker').datepicker({
        format: 'yyyy-mm-dd',
        viewMode: 'days',
        minViewMode: 'days',
        maxViewMode: 'years',
    });

    $.each(["Suppressor", "SBR", "SBS", "MG", "AOW"], function(i, p) {
        $('#nfaItemType').append($('<option></option>').val(p).html(p));
    });

    // $("#placeholder").css("height", "500px");

    var data = [];
    $.getJSON("/json", function(rows){
        $.each(rows, function (id, row) {
            data.push([row.checkCashedDate, row.approvedDate]);
        });
        $.plot("#placeholder", [{
            data: data,
            points: { show: true }
        }]);
    });
});

