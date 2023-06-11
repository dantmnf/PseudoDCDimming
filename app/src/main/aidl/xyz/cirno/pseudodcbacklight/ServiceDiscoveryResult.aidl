// ServiceDiscoveryResult.aidl
package xyz.cirno.pseudodcbacklight;

import xyz.cirno.pseudodcbacklight.IBacklightOverrideService;
// Declare any non-default types here with import statements

parcelable ServiceDiscoveryResult {
    int version;
    IBinder service;
}
