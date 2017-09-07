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
});

