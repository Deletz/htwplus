Dropzone.autoDiscover = false;

Dropzone.options.fileuploadForm = {
    init: function () {
        fileuploadForm = this;

        $("#send-button").click(function () {
            //document.getElementById("fortschrittsspalte").style.display = "inline-block";
            var files_queued = fileuploadForm.getQueuedFiles();
            //alert (files_queued.length);
            for (var i = 0; i <= files_queued.length; i++){
                //alert ("File "+i+ ": "+ files_queued[i]);
                $("#mediaList").append('<tr>'+
                '<td colspan="6"><div class="name" data-dz-name></div>' +
                '<div class="size" data-dz-size></div>' +
                '<div class="progress-bar" role="progressbar" id="total-progress" aria-valuemin="0" aria-valuemax="100" aria-valuenow="0">' +
                '</div></td></tr>');
            }
            fileuploadForm.processQueue();
            fileuploadForm.on("totaluploadprogress", function(progress) {
                document.querySelector("#total-progress .progress-bar").style.width = progress + "%";
            });

            fileuploadForm.on("sending", function(file) {
                // Show the total progress bar when upload starts
                document.querySelector("#total-progress").style.opacity = "1";
                // And disable the start button
                file.previewElement.querySelector(".start").setAttribute("disabled", "disabled");
            });
            fileuploadForm.on("complete", function (file) {
                files_queued = fileuploadForm.getQueuedFiles();
                var files_uploading = fileuploadForm.getUploadingFiles();
                alert("complete!");
                document.getElementById("loadingMedia").style.display = "inline-block";
                $("#fileuploadForm").submit();
            });

           /* fileuploadForm.on("addedfile", function(file) {
                file.previewElement.addEventListener("click", function() {
                    fileuploadForm.removeFile(file);
                });
            });

            fileuploadForm.on("complete", function (file) {
                files_queued = fileuploadForm.getQueuedFiles();
                var files_uploading = fileuploadForm.getUploadingFiles();
                fileuploadForm.removeFile(file);
            });*/


        });
    },
    previewTemplate: '<div class="dz-preview dz-file-preview">' +
    '<div class="dz-filename"><span data-dz-name></span></div>' +
    '<div class="dz-size" data-dz-size></div>' +
    //'<div class="dz-progress"><span class="dz-upload" data-dz-uploadprogress></span></div>' +
    '<div> <button data-dz-remove>Entfernen</button></div>' +
    '</div>',
    paramName: "file",
    maxFilesize: 256,
    uploadMultiple: true,
    parallelUploads: 10,
    autoProcessQueue: false
};
var myDropzone = new Dropzone(".dropzone");