// start of header
var imports = new JavaImporter(org.eigengo.activator.nashorn.japi);

with (imports) {

    function workflow() {
        return /* end of header */ {
            states: [
                {
                    name: "process-poster",
                    run: function (request, instance) {
                        instance.next("process-kitten", { one: 1, two: "two" });
                    }
                },
                {
                    name: "process-kitten",
                    run: function (request, instance) {
                        instance.next("process-final", {three: "three"});
                    }
                },
                {
                    name: "process-final",
                    run: function(request, instance) {
                        instance.end({four:{name:"four", value:4}});
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