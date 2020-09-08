@objc(PitchTracker)
class PitchTracker: RCTEventEmitter {

    @objc
    func start() {
        print("Swift> start()")
        sendEvent(withName: "NoteOn", body: ["midiNum": 3])
    }

    @objc
    func stop() {
        print("Swift> stop()")
    }

    @objc
    func prepare() {
        print("Swift> prepare()")
    }

    override func supportedEvents() -> [String]! {
        return ["NoteOn", "NoteOff"]
    }

    override static func requiresMainQueueSetup() -> Bool {
        return true;
    }
}
