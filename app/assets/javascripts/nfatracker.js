/**
 * Created by peads on 9/6/17.
 */
$(function () {
    $.each(["Suppressor", "SBR", "SBS", "MG", "AOW"], function (i, p) {
        $('#nfaItemType').append($('<option></option>').val(p).html(p));
    });
});

