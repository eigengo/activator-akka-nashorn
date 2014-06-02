function workflow() {
    var Future = org.eigengo.activator.nashorn.japi.Future;
    var String = java.lang.String;
    var RuntimeException = java.lang.RuntimeException;
    var defaultInstruction = {
        expect: { type: "image/jpeg2000", camera: "back" },
        transition: { message: "Start", delay: "3s" }
    };

    function bytesToJson(x) {
        return JSON.parse(new String(x));
    }

    function requestToBytes(x) {
        return x;
    }

    return /* end of header */ {
        states: [
            {
                name: "process-poster",
                run: function (instance, request, data) {
                    var poster = requestToBytes(request);

                    var text = Future.adapt(ocr.recognise(poster));
                    var posterKittenPrint = Future.adapt(vision.extractKitten(poster)).flatMap(executor, function (msg) {
                        var json = bytesToJson(msg);
                        if (json.kitten) return Future.adapt(biometric.encodeKitten(json.kitten));
                        else             return Future.failed(new RuntimeException("No kitten"));
                    });
                    posterKittenPrint.zip(text).onComplete2(executor,
                        function (x) { instance.next("process-kitten", { posterKittenPrint: x._1(), text: bytesToJson(x._2()) }, defaultInstruction); },
                        function (x) { instance.end({error: x.getMessage()}); }
                    );
                }
            },
            {
                name: "process-kitten",
                run: function (instance, request, data) {
                    var kitten = requestToBytes(request);

                    Future.adapt(biometric.encodeKitten(kitten)).flatMap(executor,
                        function(x) { return Future.adapt(biometric.compareKittens(x, data.posterKittenPrint)); }
                    ).onComplete2(executor,
                        function(x) { var json = bytesToJson(x); instance.end(json, json); },
                        function(x) { instance.end({error: x.getMessage()}); }
                    );
                }
            }
        ],
        initialInstruction: defaultInstruction
    } /* start of footer */ ;
}

workflow();