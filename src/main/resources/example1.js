function workflow() {
    return /* end of header */ {
        states: [
            {
                name: "process-poster",
                run: function (instance, request) {
                    var poster = request; // request.entity().data().toByteArray();

                    var text = $(ocr.recognise(poster));
                    var posterKittenPrint = $(vision.extractKitten(poster)).flatMap(function (x) {
                        if (x.isDefined()) return $(biometric.encodeKitten(x.get().kitten()));
                        else               return $failed(new java.lang.RuntimeException("No kitten"));
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
                run: function (instance, request) {
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
                run: function (instance) {

                }
            }
        ],
        initialTransition: {

        }
    } /* start of footer */ ;
}

workflow();
