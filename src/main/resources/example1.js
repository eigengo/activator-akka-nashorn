// start of header
var imports = new JavaImporter(org.eigengo.activator.nashorn.japi);

with (imports) {

    function workflow() {
        return /* end of header */ {
            states: [
                {
                    name: "process-poster",
                    run: function (request, next, data) {
                        var poster = request; // request.entity().data().toByteArray();

                        var text = Future.apply(ocr.recognise(poster));
                        var posterKittenPrint = Future.apply(vision.extractKitten(poster)).flatMap(function (x) {
                            if (x.isDefined()) return Future.apply(biometric.encodeKitten(x.get().kitten()));
                            else               return Future.failed(new java.lang.RuntimeException("No kitten"));
                        }, executor);

                        posterKittenPrint.zip(text).onComplete2(
                            function (x) {
                                next("process-kitten", { posterKittenPrint: x._1(), text: x._2() });
                            },
                            function (x) {
                                next("end", {});
                            },
                            executor
                        );
                    }
                },
                {
                    name: "process-kitten",
                    run: function (request, next, data) {
                        var kitten = request; // request.entity().data().toByteArray();
                        Future.apply(biometric.encodeKitten(kitten)).flatMap(function (x) {
                            return Future.apply(biometric.compareKittens(x, data.posterKittenPrint));
                        }, executor).onComplete2(
                            function (x) {
                                next("end", { match: x });
                            },
                            function (x) {
                                next("end", {});
                            },
                            executor
                        );
                    }
                },
                {
                    name: "end",
                    run: function (request, next, data) {

                    }
                }
            ],
            initialTransition: {

            }
        } /* start of footer */ ;
    }

    workflow();
}
// end of footer