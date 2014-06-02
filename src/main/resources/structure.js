function workflow() {
    return /* end of header */ {
        states: [
            {
                name: "process-poster",
                run: function (instance, request) {
                    instance.next("process-kitten",
                        { one: 1, two: "two" },
                        {
                            expect: { type: "video/mjpeg2000", camera: "front" },
                            transition: { message: "XXX", delay: "3s" }
                        }
                    );
                }
            },
            {
                name: "process-kitten",
                run: function (instance, request) {
                    instance.next("process-final",
                        { three: "three" },
                        {
                            expect: { type: "video/mjpeg2000", camera: "front" },
                            transition: { message: "XXX", delay: "3s" }
                        }
                    );
                }
            },
            {
                name: "process-final",
                run: function(instance, request) {
                    instance.end({four:{name:"four", value:4}}, {});
                }
            }
        ],
        initialInstruction: {
            expect: { type: "image/jpeg2000", camera: "back" },
            transition: { message: "Start", delay: "3s" }
        }
    } /* start of footer */ ;
}

workflow();