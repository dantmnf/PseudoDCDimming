// ServiceDiscoveryResult.aidl
package xyz.cirno.pseudodcdimming;

import xyz.cirno.pseudodcdimming.IBacklightOverrideService;
// Declare any non-default types here with import statements

parcelable ServiceDiscoveryResult {
    int version;
    IBinder service;
}
