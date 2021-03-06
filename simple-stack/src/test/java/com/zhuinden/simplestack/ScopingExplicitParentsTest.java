/*
 * Copyright 2018 Gabor Varadi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhuinden.simplestack;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.zhuinden.simplestack.helpers.TestKey;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ScopingExplicitParentsTest {
    private static class DefaultScopedServices
            implements ScopedServices {
        @Override
        public void bindServices(@NonNull ServiceBinder serviceBinder) {
            // boop
        }
    }

    @Test
    public void explicitParentScopesListThrowsIfNull() {
        class ChildKey
                extends TestKey
                implements ScopeKey.Child {
            ChildKey(String name) {
                super(name);
            }

            @NonNull
            @Override
            public List<String> getParentScopes() {
                return null;
            }
        }
        Backstack backstack = new Backstack();
        backstack.setScopedServices(new DefaultScopedServices());
        backstack.setup(History.of(new ChildKey("hello")));
        boolean success = false;
        try {
            backstack.setStateChanger(new StateChanger() {
                @Override
                public void handleStateChange(@NonNull StateChange stateChange, @NonNull Callback completionCallback) {
                    completionCallback.stateChangeComplete();
                }
            });
        } catch(IllegalArgumentException e) {
            success = true;
        }
        if(!success) {
            Assert.fail("The parent scope list should not be null!");
        }
    }

    @Test
    public void explicitParentsWithoutScopedServicesThrows() {
        class ChildKey
                extends TestKey
                implements ScopeKey.Child {
            ChildKey(String name) {
                super(name);
            }

            @NonNull
            @Override
            public List<String> getParentScopes() {
                return History.of("parentScope");
            }
        }
        Backstack backstack = new Backstack();
        backstack.setup(History.of(new ChildKey("hello")));
        boolean success = false;
        try {
            backstack.setStateChanger(new StateChanger() {
                @Override
                public void handleStateChange(@NonNull StateChange stateChange, @NonNull Callback completionCallback) {
                    completionCallback.stateChangeComplete();
                }
            });
        } catch(IllegalStateException e) {
            success = true;
        }
        if(!success) {
            Assert.fail("Asserting scoped services should have thrown here!");
        }
    }

    @Test
    public void explicitParentsDefinedByScopeAreCreated() {
        class ChildKey
                extends TestKey
                implements ScopeKey.Child {
            ChildKey(String name) {
                super(name);
            }

            @NonNull
            @Override
            public List<String> getParentScopes() {
                return History.of("parentScope");
            }
        }
        Backstack backstack = new Backstack();
        backstack.setScopedServices(new DefaultScopedServices());
        backstack.setup(History.of(new ChildKey("hello")));
        backstack.setStateChanger(new StateChanger() {
            @Override
            public void handleStateChange(@NonNull StateChange stateChange, @NonNull Callback completionCallback) {
                completionCallback.stateChangeComplete();
            }
        });

        assertThat(backstack.hasScope("parentScope")).isTrue();
    }

    @Test
    public void explicitParentsDefinedByMultipleKeysAreCreatedInTheRightOrder() {
        class ChildKey1
                extends TestKey
                implements ScopeKey, ScopeKey.Child {
            ChildKey1(String name) {
                super(name);
            }

            @NonNull
            @Override
            public List<String> getParentScopes() {
                return History.of("parentScope1", "parentScope2");
            }

            @NonNull
            @Override
            public String getScopeTag() {
                return name;
            }
        }

        class ChildKey2
                extends TestKey
                implements ScopeKey, ScopeKey.Child {
            ChildKey2(String name) {
                super(name);
            }

            @NonNull
            @Override
            public List<String> getParentScopes() {
                return History.of("parentScope2", "parentScope3");
            }

            @NonNull
            @Override
            public String getScopeTag() {
                return name;
            }
        }

        Backstack backstack = new Backstack();
        backstack.setScopedServices(new DefaultScopedServices());
        backstack.setup(History.of(new ChildKey1("hello"), new ChildKey2("world")));
        backstack.setStateChanger(new StateChanger() {
            @Override
            public void handleStateChange(@NonNull StateChange stateChange, @NonNull Callback completionCallback) {
                completionCallback.stateChangeComplete();
            }
        });

        assertThat(backstack.hasScope("parentScope1")).isTrue();
        assertThat(backstack.hasScope("parentScope2")).isTrue();
        assertThat(backstack.hasScope("parentScope3")).isTrue();
        assertThat(backstack.hasScope("hello")).isTrue();
        assertThat(backstack.hasScope("world")).isTrue();

        backstack.goBack();

        assertThat(backstack.hasScope("parentScope1")).isTrue();
        assertThat(backstack.hasScope("parentScope2")).isTrue();
        assertThat(backstack.hasScope("parentScope3")).isFalse();
        assertThat(backstack.hasScope("hello")).isTrue();
        assertThat(backstack.hasScope("world")).isFalse();
    }

    @Test
    public void explicitParentsArePreferredDuringScopeLookup() {
        final Object service1 = new Object();
        final Object service2 = new Object();
        final Object service3 = new Object();
        final Object service4 = new Object();

        class ChildKey
                extends TestKey
                implements ScopeKey, ScopeKey.Child {
            private final List<String> parentScopes;

            ChildKey(String name, List<String> parentScopes) {
                super(name);
                this.parentScopes = parentScopes;
            }

            @NonNull
            @Override
            public String getScopeTag() {
                return name;
            }

            @NonNull
            @Override
            public List<String> getParentScopes() {
                return parentScopes;
            }
        }

        Backstack backstack = new Backstack();
        backstack.setScopedServices(new ScopedServices() {
            @Override
            public void bindServices(@NonNull ServiceBinder serviceBinder) {
                String tag = serviceBinder.getScopeTag();
                if("hello".equals(tag)) {
                    serviceBinder.addService("SERVICE", service1);
                }
                if("parentScope1".equals(tag)) {
                    serviceBinder.addService("SERVICE", service2);
                }
                if("parentScope2".equals(tag)) {
                    serviceBinder.addService("SERVICE", service3);
                }
                if("parentScope3".equals(tag)) {
                    serviceBinder.addService("SERVICE", service4);
                }
            }
        });

        backstack.setup(History.of(
                new ChildKey("hello", History.of("parentScope1")),
                new ChildKey("world", History.of("parentScope2", "parentScope3"))
        ));
        backstack.setStateChanger(new StateChanger() {
            @Override
            public void handleStateChange(@NonNull StateChange stateChange, @NonNull Callback completionCallback) {
                completionCallback.stateChangeComplete();
            }
        });

        assertThat(backstack.hasScope("hello")).isTrue();
        assertThat(backstack.hasScope("world")).isTrue();
        assertThat(backstack.hasScope("parentScope1")).isTrue();
        assertThat(backstack.hasScope("parentScope2")).isTrue();
        assertThat(backstack.hasScope("parentScope3")).isTrue();

        assertThat(backstack.getService("hello", "SERVICE")).isSameAs(service1);
        assertThat(backstack.getService("parentScope1", "SERVICE")).isSameAs(service2);
        assertThat(backstack.getService("parentScope2", "SERVICE")).isSameAs(service3);
        assertThat(backstack.getService("parentScope3", "SERVICE")).isSameAs(service4);

        assertThat(backstack.lookupService("SERVICE")).isSameAs(service4);

        backstack.goBack();

        assertThat(backstack.lookupService("SERVICE")).isSameAs(service1);
    }

    @Test
    public void explicitParentsArePreferredDuringScopeLookupOtherSetup() {
        final Object service1 = new Object();
        final Object service2 = new Object();

        class ChildKey
                extends TestKey
                implements ScopeKey, ScopeKey.Child {
            private final List<String> parentScopes;

            ChildKey(String name, List<String> parentScopes) {
                super(name);
                this.parentScopes = parentScopes;
            }

            @NonNull
            @Override
            public String getScopeTag() {
                return name;
            }

            @NonNull
            @Override
            public List<String> getParentScopes() {
                return parentScopes;
            }
        }

        Backstack backstack = new Backstack();
        backstack.setScopedServices(new ScopedServices() {
            @Override
            public void bindServices(@NonNull ServiceBinder serviceBinder) {
                String tag = serviceBinder.getScopeTag();
                if("hello".equals(tag)) {
                    serviceBinder.addService("SERVICE", service1);
                }
                if("parentScope1".equals(tag)) {
                    serviceBinder.addService("SERVICE", service2);
                }
            }
        });

        backstack.setup(History.of(
                new ChildKey("hello", History.of("parentScope1")),
                new ChildKey("world", History.of("parentScope1"))
        ));
        backstack.setStateChanger(new StateChanger() {
            @Override
            public void handleStateChange(@NonNull StateChange stateChange, @NonNull Callback completionCallback) {
                completionCallback.stateChangeComplete();
            }
        });

        assertThat(backstack.hasScope("hello")).isTrue();
        assertThat(backstack.hasScope("world")).isTrue();
        assertThat(backstack.hasScope("parentScope1")).isTrue();

        assertThat(backstack.getService("hello", "SERVICE")).isSameAs(service1);
        assertThat(backstack.getService("parentScope1", "SERVICE")).isSameAs(service2);

        assertThat(backstack.lookupService("SERVICE")).isSameAs(service2);

        backstack.goBack();

        assertThat(backstack.lookupService("SERVICE")).isSameAs(service1);
    }

    @Test
    public void explicitParentsAreLookedUpEvenInPreviousKeysDuringLookup() {
        final Object service1 = new Object();

        class ChildKey
                extends TestKey
                implements ScopeKey, ScopeKey.Child {
            private final List<String> parentScopes;

            ChildKey(String name, List<String> parentScopes) {
                super(name);
                this.parentScopes = parentScopes;
            }

            @NonNull
            @Override
            public String getScopeTag() {
                return name;
            }

            @NonNull
            @Override
            public List<String> getParentScopes() {
                return parentScopes;
            }
        }

        Backstack backstack = new Backstack();
        backstack.setScopedServices(new ScopedServices() {
            @Override
            public void bindServices(@NonNull ServiceBinder serviceBinder) {
                if("parentScope1".equals(serviceBinder.getScopeTag())) {
                    serviceBinder.addService("service", service1);
                }
            }
        });

        backstack.setup(History.of(
                new ChildKey("hello", History.of("parentScope1")),
                new ChildKey("world", History.of("parentScope2", "parentScope3"))
        ));
        backstack.setStateChanger(new StateChanger() {
            @Override
            public void handleStateChange(@NonNull StateChange stateChange, @NonNull Callback completionCallback) {
                completionCallback.stateChangeComplete();
            }
        });

        assertThat(backstack.hasScope("parentScope1")).isTrue();
        assertThat(backstack.hasScope("parentScope2")).isTrue();
        assertThat(backstack.hasScope("parentScope3")).isTrue();
        assertThat(backstack.hasScope("hello")).isTrue();
        assertThat(backstack.hasScope("world")).isTrue();

        assertThat(backstack.lookupService("service")).isSameAs(service1);
    }

    @Test
    public void explicitParentsAreDestroyedWhileScopesAreFinalized() {
        Activity activity = Mockito.mock(Activity.class);
        Mockito.when(activity.isFinishing()).thenReturn(true);

        final List<Object> registered = new ArrayList<>();
        final List<Object> unregistered = new ArrayList<>();

        final List<Object> activated = new ArrayList<>();
        final List<Object> inactivated = new ArrayList<>();
        class Service
                implements ScopedServices.Registered, ScopedServices.Activated {
            private boolean didServiceRegister;
            private boolean didServiceUnregister;
            private boolean didServiceActivate;
            private boolean didServiceInactivate;

            @Override
            public void onServiceActive() {
                this.didServiceActivate = true;
                activated.add(this);
            }

            @Override
            public void onServiceInactive() {
                this.didServiceInactivate = true;
                inactivated.add(this);
            }

            @Override
            public void onServiceRegistered() {
                this.didServiceRegister = true;
                registered.add(this);
            }

            @Override
            public void onServiceUnregistered() {
                this.didServiceUnregister = true;
                unregistered.add(this);
            }
        }
        final Service service = new Service();

        class ChildKey
                extends TestKey
                implements ScopeKey, ScopeKey.Child {
            private final List<String> parentScopes;

            ChildKey(String name, List<String> parentScopes) {
                super(name);
                this.parentScopes = parentScopes;
            }

            @NonNull
            @Override
            public String getScopeTag() {
                return name;
            }

            @NonNull
            @Override
            public List<String> getParentScopes() {
                return parentScopes;
            }
        }

        StateChanger stateChanger = new StateChanger() {
            @Override
            public void handleStateChange(@NonNull StateChange stateChange, @NonNull Callback completionCallback) {
                completionCallback.stateChangeComplete();
            }
        };
        BackstackDelegate backstackDelegate = new BackstackDelegate();
        backstackDelegate.setScopedServices(activity, new ScopedServices() {
            @Override
            public void bindServices(@NonNull ServiceBinder serviceBinder) {
                if("parentScope2".equals(serviceBinder.getScopeTag())) {
                    serviceBinder.addService("service", service);
                }
            }
        });
        backstackDelegate.onCreate(null, null, History.of(
                new ChildKey("hello", History.of("parentScope1")),
                new ChildKey("world", History.of("parentScope2", "parentScope3"))
        ));
        backstackDelegate.setStateChanger(stateChanger);

        backstackDelegate.onPostResume();
        backstackDelegate.onPause();

        assertThat(backstackDelegate.hasScope("hello")).isTrue();
        assertThat(backstackDelegate.hasScope("world")).isTrue();
        assertThat(backstackDelegate.hasScope("parentScope1")).isTrue();
        assertThat(backstackDelegate.hasScope("parentScope2")).isTrue();
        assertThat(backstackDelegate.hasScope("parentScope3")).isTrue();
        assertThat(backstackDelegate.hasService("parentScope2", "service")).isTrue();
        assertThat(service.didServiceRegister).isTrue();
        assertThat(service.didServiceActivate).isTrue();
        assertThat(service.didServiceInactivate).isFalse();
        assertThat(service.didServiceUnregister).isFalse();
        backstackDelegate.onDestroy();
        assertThat(backstackDelegate.hasScope("hello")).isFalse();
        assertThat(backstackDelegate.hasScope("world")).isFalse();
        assertThat(backstackDelegate.hasScope("parentScope1")).isFalse();
        assertThat(backstackDelegate.hasScope("parentScope2")).isFalse();
        assertThat(backstackDelegate.hasScope("parentScope3")).isFalse();
        assertThat(backstackDelegate.hasService("parentScope2", "service")).isFalse();
        assertThat(service.didServiceRegister).isTrue();
        assertThat(service.didServiceActivate).isTrue();
        assertThat(service.didServiceInactivate).isTrue();
        assertThat(service.didServiceUnregister).isTrue();

        assertThat(registered).containsExactly(service);
        assertThat(activated).containsExactly(service);
        assertThat(inactivated).containsExactly(service);
        assertThat(unregistered).containsExactly(service);
    }

    @Test
    public void explicitParentsAreDestroyedIfNoScopeKeyKeepsThemAlive() {

        final List<Object> serviceRegistered = new ArrayList<>();
        final List<Object> serviceUnregistered = new ArrayList<>();

        final List<Object> activated = new ArrayList<>();
        final List<Object> inactivated = new ArrayList<>();
        class Service
                implements ScopedServices.Registered, ScopedServices.Activated {
            private boolean didServiceRegistered;
            private boolean didServiceUnregistered;
            private boolean didServiceActivate;
            private boolean didServiceInactivate;

            @Override
            public void onServiceActive() {
                this.didServiceActivate = true;
                activated.add(this);
            }

            @Override
            public void onServiceInactive() {
                this.didServiceInactivate = true;
                inactivated.add(this);
            }

            @Override
            public void onServiceRegistered() {
                this.didServiceRegistered = true;
                serviceRegistered.add(this);
            }

            @Override
            public void onServiceUnregistered() {
                this.didServiceUnregistered = true;
                serviceUnregistered.add(this);
            }
        }
        final Service service = new Service();

        class ChildKey
                extends TestKey
                implements ScopeKey, ScopeKey.Child {
            private final List<String> parentScopes;

            ChildKey(String name, List<String> parentScopes) {
                super(name);
                this.parentScopes = parentScopes;
            }

            @NonNull
            @Override
            public String getScopeTag() {
                return name;
            }

            @NonNull
            @Override
            public List<String> getParentScopes() {
                return parentScopes;
            }
        }

        StateChanger stateChanger = new StateChanger() {
            @Override
            public void handleStateChange(@NonNull StateChange stateChange, @NonNull Callback completionCallback) {
                completionCallback.stateChangeComplete();
            }
        };
        Backstack backstack = new Backstack();
        backstack.setScopedServices(new ScopedServices() {
            @Override
            public void bindServices(@NonNull ServiceBinder serviceBinder) {
                if("parentScope".equals(serviceBinder.getScopeTag())) {
                    serviceBinder.addService("service", service);
                }
            }
        });
        backstack.setup(History.of(
                new ChildKey("hello", History.of("parentScope1")),
                new ChildKey("world", History.of("parentScope")),
                new ChildKey("bye", History.of("parentScope"))
        ));
        backstack.setStateChanger(stateChanger);

        assertThat(backstack.hasScope("parentScope")).isTrue();
        assertThat(backstack.lookupService("service")).isSameAs(service);
        assertThat(backstack.getService("parentScope", "service")).isSameAs(service);

        backstack.goBack();

        assertThat(backstack.hasScope("parentScope")).isTrue();
        assertThat(backstack.lookupService("service")).isSameAs(service);
        assertThat(backstack.getService("parentScope", "service")).isSameAs(service);

        backstack.goBack();

        assertThat(backstack.hasScope("parentScope")).isFalse();
        assertThat(backstack.canFindService("service")).isFalse();
    }

    @Test
    public void explicitParentServicesAreInitializedBeforeActualScopeKeyServices() {
        final List<Object> serviceRegistered = new ArrayList<>();
        final List<Object> serviceUnregistered = new ArrayList<>();

        final List<Object> activated = new ArrayList<>();
        final List<Object> inactivated = new ArrayList<>();
        class Service
                implements ScopedServices.Registered, ScopedServices.Activated {
            private boolean didServiceRegistered;
            private boolean didServiceUnregistered;
            private boolean didServiceActivate;
            private boolean didServiceInactivate;

            @Override
            public void onServiceActive() {
                this.didServiceActivate = true;
                activated.add(this);
            }

            @Override
            public void onServiceInactive() {
                this.didServiceInactivate = true;
                inactivated.add(this);
            }

            @Override
            public void onServiceRegistered() {
                this.didServiceRegistered = true;
                serviceRegistered.add(this);
            }

            @Override
            public void onServiceUnregistered() {
                this.didServiceUnregistered = true;
                serviceUnregistered.add(this);
            }
        }
        final Service service1 = new Service();
        final Service service2 = new Service();

        class ChildKey
                extends TestKey
                implements ScopeKey, ScopeKey.Child {
            private final List<String> parentScopes;

            ChildKey(String name, List<String> parentScopes) {
                super(name);
                this.parentScopes = parentScopes;
            }

            @NonNull
            @Override
            public String getScopeTag() {
                return name;
            }

            @NonNull
            @Override
            public List<String> getParentScopes() {
                return parentScopes;
            }
        }

        StateChanger stateChanger = new StateChanger() {
            @Override
            public void handleStateChange(@NonNull StateChange stateChange, @NonNull Callback completionCallback) {
                completionCallback.stateChangeComplete();
            }
        };
        Backstack backstack = new Backstack();
        backstack.setScopedServices(new ScopedServices() {
            @Override
            public void bindServices(@NonNull ServiceBinder serviceBinder) {
                if(serviceBinder.getScopeTag().equals("parentScope")) {
                    serviceBinder.addService("service", service1);
                }
                if(serviceBinder.getScopeTag().equals("hello")) {
                    serviceBinder.addService("service", service2);
                }
            }
        });
        backstack.setup(History.of(
                new ChildKey("hello", History.of("parentScope"))
        ));
        backstack.setStateChanger(stateChanger);

        assertThat(backstack.hasScope("hello")).isTrue();
        assertThat(backstack.hasScope("parentScope")).isTrue();
        assertThat(backstack.hasService("hello", "service")).isTrue();
        assertThat(backstack.hasService("parentScope", "service")).isTrue();
        assertThat(backstack.getService("hello", "service")).isSameAs(service2);
        assertThat(backstack.getService("parentScope", "service")).isSameAs(service1);

        assertThat(serviceRegistered).containsExactly(service1, service2);
        assertThat(activated).containsExactly(service1, service2);

        backstack.setHistory(
                History.of(new ChildKey("bye", History.of("boop"))),
                StateChange.BACKWARD);

        assertThat(serviceUnregistered).containsExactly(service2, service1);
        assertThat(inactivated).containsExactly(service2, service1);
    }

    @Test
    public void servicesInExplicitParentsAreDestroyedOnlyAfterAllChildServicesAreDestroyed() {
        final List<Object> registered = new ArrayList<>();
        final List<Object> unregistered = new ArrayList<>();

        final List<Object> activated = new ArrayList<>();
        final List<Object> inactivated = new ArrayList<>();
        class Service
                implements ScopedServices.Registered, ScopedServices.Activated {
            private boolean didServiceRegister;
            private boolean didServiceUnregister;
            private boolean didServiceActivate;
            private boolean didServiceInactivate;

            @Override
            public void onServiceActive() {
                this.didServiceActivate = true;
                activated.add(this);
            }

            @Override
            public void onServiceInactive() {
                this.didServiceInactivate = true;
                inactivated.add(this);
            }

            @Override
            public void onServiceRegistered() {
                this.didServiceRegister = true;
                registered.add(this);
            }

            @Override
            public void onServiceUnregistered() {
                this.didServiceUnregister = true;
                unregistered.add(this);
            }
        }
        final Service service1 = new Service();
        final Service service2 = new Service();

        final Service service3 = new Service();
        final Service service4 = new Service();

        class ChildKey
                extends TestKey
                implements ScopeKey, ScopeKey.Child {
            private final List<String> parentScopes;

            ChildKey(String name, List<String> parentScopes) {
                super(name);
                this.parentScopes = parentScopes;
            }

            @NonNull
            @Override
            public String getScopeTag() {
                return name;
            }

            @NonNull
            @Override
            public List<String> getParentScopes() {
                return parentScopes;
            }
        }

        StateChanger stateChanger = new StateChanger() {
            @Override
            public void handleStateChange(@NonNull StateChange stateChange, @NonNull Callback completionCallback) {
                completionCallback.stateChangeComplete();
            }
        };

        Backstack backstack = new Backstack();
        backstack.setScopedServices(new ScopedServices() {
            @Override
            public void bindServices(@NonNull ServiceBinder serviceBinder) {
                if(serviceBinder.getScopeTag().equals("parentScope1")) {
                    serviceBinder.addService("service1", service1);
                }
                if(serviceBinder.getScopeTag().equals("parentScope2")) {
                    serviceBinder.addService("service2", service2);
                }

                if(serviceBinder.getScopeTag().equals("hello")) {
                    serviceBinder.addService("service3", service3);
                }
                if(serviceBinder.getScopeTag().equals("world")) {
                    serviceBinder.addService("service4", service4);
                }
            }
        });
        backstack.setup(
                History.of(
                        new ChildKey("hello", History.of("parentScope1", "parentScope2")),
                        new ChildKey("world", History.of("parentScope2"))
                )
        );
        backstack.setStateChanger(stateChanger);

        assertThat(backstack.hasScope("parentScope1")).isTrue();
        assertThat(backstack.hasScope("parentScope2")).isTrue();
        assertThat(backstack.hasScope("hello")).isTrue();
        assertThat(backstack.hasScope("world")).isTrue();
        assertThat(backstack.hasService("parentScope2", "service2")).isTrue();
        assertThat(backstack.hasService("parentScope1", "service1")).isTrue();
        assertThat(backstack.hasService("world", "service4")).isTrue();
        assertThat(backstack.hasService("hello", "service3")).isTrue();

        backstack.setHistory(
                History.of(new ChildKey("bye", History.of("boop"))),
                StateChange.BACKWARD);

        assertThat(backstack.hasScope("parentScope1")).isFalse();
        assertThat(backstack.hasScope("parentScope2")).isFalse();
        assertThat(backstack.hasScope("hello")).isFalse();
        assertThat(backstack.hasScope("world")).isFalse();
        assertThat(backstack.hasService("parentScope2", "service2")).isFalse();
        assertThat(backstack.hasService("parentScope1", "service1")).isFalse();
        assertThat(backstack.hasService("world", "service4")).isFalse();
        assertThat(backstack.hasService("hello", "service3")).isFalse();

        assertThat(registered).containsExactly(service1, service2, service3, service4);
        assertThat(activated).containsExactly(service2, service4);
        assertThat(inactivated).containsExactly(service4, service2);
        assertThat(unregistered).containsExactly(service4, service3, service2, service1);
    }

    @Test
    public void explicitParentServicesReceiveCallbacksBeforeChildInAscendingOrder() {
        final List<Object> registered = new ArrayList<>();
        final List<Object> unregistered = new ArrayList<>();

        final List<Object> activated = new ArrayList<>();
        final List<Object> inactivated = new ArrayList<>();
        class Service
                implements ScopedServices.Registered, ScopedServices.Activated {
            private boolean didServiceRegister;
            private boolean didExitScope;
            private boolean didServiceActivate;
            private boolean didServiceInactivate;

            @Override
            public void onServiceActive() {
                this.didServiceActivate = true;
                activated.add(this);
            }

            @Override
            public void onServiceInactive() {
                this.didServiceInactivate = true;
                inactivated.add(this);
            }

            @Override
            public void onServiceRegistered() {
                this.didServiceRegister = true;
                registered.add(this);
            }

            @Override
            public void onServiceUnregistered() {
                this.didExitScope = true;
                unregistered.add(this);
            }
        }
        final Service serviceP0 = new Service();
        final Service serviceP1 = new Service();
        final Service serviceP2 = new Service();
        final Service serviceP3 = new Service();
        final Service serviceP4 = new Service();
        final Service serviceC1 = new Service();
        final Service serviceC2 = new Service();
        final Service serviceC3 = new Service();
        final Service serviceC4 = new Service();
        final Service serviceC5 = new Service();

        class ChildKey
                extends TestKey
                implements ScopeKey, ScopeKey.Child {
            private final List<String> parentScopes;

            ChildKey(String name, List<String> parentScopes) {
                super(name);
                this.parentScopes = parentScopes;
            }

            @NonNull
            @Override
            public String getScopeTag() {
                return name;
            }

            @NonNull
            @Override
            public List<String> getParentScopes() {
                return parentScopes;
            }
        }

        StateChanger stateChanger = new StateChanger() {
            @Override
            public void handleStateChange(@NonNull StateChange stateChange, @NonNull Callback completionCallback) {
                completionCallback.stateChangeComplete();
            }
        };

        Backstack backstack = new Backstack();
        backstack.setScopedServices(new ScopedServices() {
            @Override
            public void bindServices(@NonNull ServiceBinder serviceBinder) {
                if(serviceBinder.getScopeTag().equals("P0")) {
                    serviceBinder.addService("service", serviceP0);
                }
                if(serviceBinder.getScopeTag().equals("P1")) {
                    serviceBinder.addService("service", serviceP1);
                }
                if(serviceBinder.getScopeTag().equals("P2")) {
                    serviceBinder.addService("service", serviceP2);
                }
                if(serviceBinder.getScopeTag().equals("P3")) {
                    serviceBinder.addService("service", serviceP3);
                }
                if(serviceBinder.getScopeTag().equals("P4")) {
                    serviceBinder.addService("service", serviceP4);
                }
                if(serviceBinder.getScopeTag().equals("C1")) {
                    serviceBinder.addService("service", serviceC1);
                }
                if(serviceBinder.getScopeTag().equals("C2")) {
                    serviceBinder.addService("service", serviceC2);
                }
                if(serviceBinder.getScopeTag().equals("C3")) {
                    serviceBinder.addService("service", serviceC3);
                }
                if(serviceBinder.getScopeTag().equals("C4")) {
                    serviceBinder.addService("service", serviceC4);
                }
                if(serviceBinder.getScopeTag().equals("C5")) {
                    serviceBinder.addService("service", serviceC5);
                }
            }
        });

        /*
         *                    PARENT0
         *            PARENT1
         *     PARENT2       PARENT3         PARENT4
         *   CHILD1 CHILD2    CHILD3     CHILD4   CHILD5
         */
        backstack.setup(
                History.of(
                        new ChildKey("C1", History.of("P0", "P1", "P2")),
                        new ChildKey("C2", History.of("P0", "P1", "P2")),
                        new ChildKey("C3", History.of("P0", "P1", "P3")),
                        new ChildKey("C4", History.of("P0", "P4")),
                        new ChildKey("C5", History.of("P0", "P4"))
                )
        );
        backstack.setStateChanger(stateChanger);

        assertThat(backstack.hasScope("C1")).isTrue();
        assertThat(backstack.hasScope("C2")).isTrue();
        assertThat(backstack.hasScope("C3")).isTrue();
        assertThat(backstack.hasScope("C4")).isTrue();
        assertThat(backstack.hasScope("C5")).isTrue();
        assertThat(backstack.hasScope("P0")).isTrue();
        assertThat(backstack.hasScope("P1")).isTrue();
        assertThat(backstack.hasScope("P2")).isTrue();
        assertThat(backstack.hasScope("P3")).isTrue();
        assertThat(backstack.hasScope("P4")).isTrue();

        assertThat(backstack.getService("C1", "service")).isSameAs(serviceC1);
        assertThat(backstack.getService("C2", "service")).isSameAs(serviceC2);
        assertThat(backstack.getService("C3", "service")).isSameAs(serviceC3);
        assertThat(backstack.getService("C4", "service")).isSameAs(serviceC4);
        assertThat(backstack.getService("C5", "service")).isSameAs(serviceC5);
        assertThat(backstack.getService("P0", "service")).isSameAs(serviceP0);
        assertThat(backstack.getService("P1", "service")).isSameAs(serviceP1);
        assertThat(backstack.getService("P2", "service")).isSameAs(serviceP2);
        assertThat(backstack.getService("P3", "service")).isSameAs(serviceP3);
        assertThat(backstack.getService("P4", "service")).isSameAs(serviceP4);

        /// verified set up

        assertThat(registered).containsExactly(serviceP0,
                serviceP1,
                serviceP2,
                serviceC1,
                serviceC2,
                serviceP3,
                serviceC3,
                serviceP4,
                serviceC4,
                serviceC5);
        assertThat(activated).containsExactly(serviceP0, serviceP4, serviceC5);
        assertThat(inactivated).isEmpty();
        assertThat(unregistered).isEmpty();

        backstack.goBack(); // [C1, C2, C3, C4]

        assertThat(activated).containsExactly(serviceP0, serviceP4, serviceC5, serviceC4);
        assertThat(registered).containsExactly(serviceP0,
                serviceP1,
                serviceP2,
                serviceC1,
                serviceC2,
                serviceP3,
                serviceC3,
                serviceP4,
                serviceC4,
                serviceC5);
        assertThat(inactivated).containsExactly(serviceC5);
        assertThat(unregistered).containsExactly(serviceC5);

        backstack.goBack(); // [C1, C2, C3]

        assertThat(activated).containsExactly(serviceP0,
                serviceP4,
                serviceC5,
                serviceC4,
                serviceP1,
                serviceP3,
                serviceC3);
        assertThat(registered).containsExactly(serviceP0,
                serviceP1,
                serviceP2,
                serviceC1,
                serviceC2,
                serviceP3,
                serviceC3,
                serviceP4,
                serviceC4,
                serviceC5);
        assertThat(inactivated).containsExactly(serviceC5, serviceC4, serviceP4);
        assertThat(unregistered).containsExactly(serviceC5, serviceC4, serviceP4);

        backstack.jumpToRoot(); // [C1]

        assertThat(activated).containsExactly(serviceP0,
                serviceP4,
                serviceC5,
                serviceC4,
                serviceP1,
                serviceP3,
                serviceC3,
                serviceP2,
                serviceC1);
        assertThat(registered).containsExactly(serviceP0,
                serviceP1,
                serviceP2,
                serviceC1,
                serviceC2,
                serviceP3,
                serviceC3,
                serviceP4,
                serviceC4,
                serviceC5);
        assertThat(inactivated).containsExactly(serviceC5, serviceC4, serviceP4, serviceC3, serviceP3);
        assertThat(unregistered).containsExactly(serviceC5, serviceC4, serviceP4, serviceC3, serviceP3, serviceC2);

        backstack.setHistory(History.of(new TestKey("bye")), StateChange.REPLACE); // ["bye"]

        assertThat(activated).containsExactly(serviceP0,
                serviceP4,
                serviceC5,
                serviceC4,
                serviceP1,
                serviceP3,
                serviceC3,
                serviceP2,
                serviceC1);
        assertThat(registered).containsExactly(serviceP0,
                serviceP1,
                serviceP2,
                serviceC1,
                serviceC2,
                serviceP3,
                serviceC3,
                serviceP4,
                serviceC4,
                serviceC5);
        assertThat(inactivated).containsExactly(serviceC5,
                serviceC4,
                serviceP4,
                serviceC3,
                serviceP3,
                serviceC1,
                serviceP2,
                serviceP1,
                serviceP0);
        assertThat(unregistered).containsExactly(serviceC5,
                serviceC4,
                serviceP4,
                serviceC3,
                serviceP3,
                serviceC2,
                serviceC1,
                serviceP2,
                serviceP1,
                serviceP0);
    }

    @Test
    public void explicitParentServicesReceiveCallbacksBeforeChildInAscendingOrderOtherSetup() {
        final List<Object> serviceRegistered = new ArrayList<>();
        final List<Object> serviceUnregistered = new ArrayList<>();

        final List<Object> activated = new ArrayList<>();
        final List<Object> inactivated = new ArrayList<>();
        class Service
                implements ScopedServices.Registered, ScopedServices.Activated {
            private boolean didServiceRegister;
            private boolean didServiceUnregister;
            private boolean didServiceActivate;
            private boolean didServiceInactivate;

            @Override
            public void onServiceActive() {
                this.didServiceActivate = true;
                activated.add(this);
            }

            @Override
            public void onServiceInactive() {
                this.didServiceInactivate = true;
                inactivated.add(this);
            }

            @Override
            public void onServiceRegistered() {
                this.didServiceRegister = true;
                serviceRegistered.add(this);
            }

            @Override
            public void onServiceUnregistered() {
                this.didServiceUnregister = true;
                serviceUnregistered.add(this);
            }
        }
        final Service serviceP0 = new Service();
        final Service serviceP1 = new Service();
        final Service serviceP2 = new Service();
        final Service serviceP3 = new Service();
        final Service serviceP4 = new Service();
        final Service serviceC1 = new Service();
        final Service serviceC2 = new Service();
        final Service serviceC3 = new Service();
        final Service serviceC4 = new Service();
        final Service serviceC5 = new Service();

        class ChildKey
                extends TestKey
                implements ScopeKey, ScopeKey.Child {
            private final List<String> parentScopes;

            ChildKey(String name, List<String> parentScopes) {
                super(name);
                this.parentScopes = parentScopes;
            }

            @NonNull
            @Override
            public String getScopeTag() {
                return name;
            }

            @NonNull
            @Override
            public List<String> getParentScopes() {
                return parentScopes;
            }
        }

        StateChanger stateChanger = new StateChanger() {
            @Override
            public void handleStateChange(@NonNull StateChange stateChange, @NonNull Callback completionCallback) {
                completionCallback.stateChangeComplete();
            }
        };

        Backstack backstack = new Backstack();
        backstack.setScopedServices(new ScopedServices() {
            @Override
            public void bindServices(@NonNull ServiceBinder serviceBinder) {
                if(serviceBinder.getScopeTag().equals("P0")) {
                    serviceBinder.addService("service", serviceP0);
                }
                if(serviceBinder.getScopeTag().equals("P1")) {
                    serviceBinder.addService("service", serviceP1);
                }
                if(serviceBinder.getScopeTag().equals("P2")) {
                    serviceBinder.addService("service", serviceP2);
                }
                if(serviceBinder.getScopeTag().equals("P3")) {
                    serviceBinder.addService("service", serviceP3);
                }
                if(serviceBinder.getScopeTag().equals("P4")) {
                    serviceBinder.addService("service", serviceP4);
                }
                if(serviceBinder.getScopeTag().equals("C1")) {
                    serviceBinder.addService("service", serviceC1);
                }
                if(serviceBinder.getScopeTag().equals("C2")) {
                    serviceBinder.addService("service", serviceC2);
                }
                if(serviceBinder.getScopeTag().equals("C3")) {
                    serviceBinder.addService("service", serviceC3);
                }
                if(serviceBinder.getScopeTag().equals("C4")) {
                    serviceBinder.addService("service", serviceC4);
                }
                if(serviceBinder.getScopeTag().equals("C5")) {
                    serviceBinder.addService("service", serviceC5);
                }
            }
        });

        /*
         *                    PARENT0       PARENT4
         *            PARENT1           CHILD4   CHILD5
         *     PARENT2       PARENT3
         *   CHILD1 CHILD2    CHILD3
         */
        backstack.setup(
                History.of(
                        new ChildKey("C1", History.of("P0", "P1", "P2")),
                        new ChildKey("C2", History.of("P0", "P1", "P2")),
                        new ChildKey("C3", History.of("P0", "P1", "P3")),
                        new ChildKey("C4", History.of("P4")),
                        new ChildKey("C5", History.of("P4"))
                )
        );
        backstack.setStateChanger(stateChanger);

        assertThat(backstack.hasScope("C1")).isTrue();
        assertThat(backstack.hasScope("C2")).isTrue();
        assertThat(backstack.hasScope("C3")).isTrue();
        assertThat(backstack.hasScope("C4")).isTrue();
        assertThat(backstack.hasScope("C5")).isTrue();
        assertThat(backstack.hasScope("P0")).isTrue();
        assertThat(backstack.hasScope("P1")).isTrue();
        assertThat(backstack.hasScope("P2")).isTrue();
        assertThat(backstack.hasScope("P3")).isTrue();
        assertThat(backstack.hasScope("P4")).isTrue();

        assertThat(backstack.getService("C1", "service")).isSameAs(serviceC1);
        assertThat(backstack.getService("C2", "service")).isSameAs(serviceC2);
        assertThat(backstack.getService("C3", "service")).isSameAs(serviceC3);
        assertThat(backstack.getService("C4", "service")).isSameAs(serviceC4);
        assertThat(backstack.getService("C5", "service")).isSameAs(serviceC5);
        assertThat(backstack.getService("P0", "service")).isSameAs(serviceP0);
        assertThat(backstack.getService("P1", "service")).isSameAs(serviceP1);
        assertThat(backstack.getService("P2", "service")).isSameAs(serviceP2);
        assertThat(backstack.getService("P3", "service")).isSameAs(serviceP3);
        assertThat(backstack.getService("P4", "service")).isSameAs(serviceP4);

        /// verified set up

        assertThat(serviceRegistered).containsExactly(serviceP0,
                serviceP1,
                serviceP2,
                serviceC1,
                serviceC2,
                serviceP3,
                serviceC3,
                serviceP4,
                serviceC4,
                serviceC5);
        assertThat(activated).containsExactly(serviceP4, serviceC5);
        assertThat(inactivated).isEmpty();
        assertThat(serviceUnregistered).isEmpty();

        backstack.goBack(); // [C1, C2, C3, C4]

        assertThat(activated).containsExactly(serviceP4, serviceC5, serviceC4);
        assertThat(serviceRegistered).containsExactly(serviceP0,
                serviceP1,
                serviceP2,
                serviceC1,
                serviceC2,
                serviceP3,
                serviceC3,
                serviceP4,
                serviceC4,
                serviceC5);
        assertThat(inactivated).containsExactly(serviceC5);
        assertThat(serviceUnregistered).containsExactly(serviceC5);

        backstack.goBack(); // [C1, C2, C3]

        assertThat(activated).containsExactly(serviceP4,
                serviceC5,
                serviceC4,
                serviceP0,
                serviceP1,
                serviceP3,
                serviceC3);
        assertThat(serviceRegistered).containsExactly(serviceP0,
                serviceP1,
                serviceP2,
                serviceC1,
                serviceC2,
                serviceP3,
                serviceC3,
                serviceP4,
                serviceC4,
                serviceC5);
        assertThat(inactivated).containsExactly(serviceC5, serviceC4, serviceP4);
        assertThat(serviceUnregistered).containsExactly(serviceC5, serviceC4, serviceP4);

        backstack.jumpToRoot(); // [C1]

        assertThat(activated).containsExactly(serviceP4,
                serviceC5,
                serviceC4,
                serviceP0,
                serviceP1,
                serviceP3,
                serviceC3,
                serviceP2,
                serviceC1);
        assertThat(serviceRegistered).containsExactly(serviceP0,
                serviceP1,
                serviceP2,
                serviceC1,
                serviceC2,
                serviceP3,
                serviceC3,
                serviceP4,
                serviceC4,
                serviceC5);
        assertThat(inactivated).containsExactly(serviceC5, serviceC4, serviceP4, serviceC3, serviceP3);
        assertThat(serviceUnregistered).containsExactly(serviceC5, serviceC4, serviceP4, serviceC3, serviceP3, serviceC2);

        backstack.setHistory(History.of(new TestKey("bye")), StateChange.REPLACE); // ["bye"]

        assertThat(activated).containsExactly(serviceP4,
                serviceC5,
                serviceC4,
                serviceP0,
                serviceP1,
                serviceP3,
                serviceC3,
                serviceP2,
                serviceC1);
        assertThat(serviceRegistered).containsExactly(serviceP0,
                serviceP1,
                serviceP2,
                serviceC1,
                serviceC2,
                serviceP3,
                serviceC3,
                serviceP4,
                serviceC4,
                serviceC5);
        assertThat(inactivated).containsExactly(serviceC5,
                serviceC4,
                serviceP4,
                serviceC3,
                serviceP3,
                serviceC1,
                serviceP2,
                serviceP1,
                serviceP0);
        assertThat(serviceUnregistered).containsExactly(serviceC5,
                serviceC4,
                serviceP4,
                serviceC3,
                serviceP3,
                serviceC2,
                serviceC1,
                serviceP2,
                serviceP1,
                serviceP0);

        backstack.setHistory(History.of(new ChildKey("C5", History.of("P4"))),
                StateChange.REPLACE);

        assertThat(activated).containsExactly(serviceP4,
                serviceC5,
                serviceC4,
                serviceP0,
                serviceP1,
                serviceP3,
                serviceC3,
                serviceP2,
                serviceC1,
                serviceP4,
                serviceC5);
        assertThat(serviceRegistered).containsExactly(serviceP0,
                serviceP1,
                serviceP2,
                serviceC1,
                serviceC2,
                serviceP3,
                serviceC3,
                serviceP4,
                serviceC4,
                serviceC5,
                serviceP4,
                serviceC5);
        assertThat(inactivated).containsExactly(serviceC5,
                serviceC4,
                serviceP4,
                serviceC3,
                serviceP3,
                serviceC1,
                serviceP2,
                serviceP1,
                serviceP0);
        assertThat(serviceUnregistered).containsExactly(serviceC5,
                serviceC4,
                serviceP4,
                serviceC3,
                serviceP3,
                serviceC2,
                serviceC1,
                serviceP2,
                serviceP1,
                serviceP0);

        backstack.detachStateChanger();
        backstack.setStateChanger(stateChanger);

        // no change should happen here
        assertThat(activated).containsExactly(serviceP4,
                serviceC5,
                serviceC4,
                serviceP0,
                serviceP1,
                serviceP3,
                serviceC3,
                serviceP2,
                serviceC1,
                serviceP4,
                serviceC5);
        assertThat(serviceRegistered).containsExactly(serviceP0,
                serviceP1,
                serviceP2,
                serviceC1,
                serviceC2,
                serviceP3,
                serviceC3,
                serviceP4,
                serviceC4,
                serviceC5,
                serviceP4,
                serviceC5);
        assertThat(inactivated).containsExactly(serviceC5,
                serviceC4,
                serviceP4,
                serviceC3,
                serviceP3,
                serviceC1,
                serviceP2,
                serviceP1,
                serviceP0);
        assertThat(serviceUnregistered).containsExactly(serviceC5,
                serviceC4,
                serviceP4,
                serviceC3,
                serviceP3,
                serviceC2,
                serviceC1,
                serviceP2,
                serviceP1,
                serviceP0);
    }

    @Test
    public void explicitParentsAreCreatedEvenIfThereAreNoScopeKeys() {
        final List<Object> serviceRegistered = new ArrayList<>();
        final List<Object> serviceUnregistered = new ArrayList<>();

        final List<Object> activated = new ArrayList<>();
        final List<Object> inactivated = new ArrayList<>();

        class Service
                implements ScopedServices.Registered, ScopedServices.Activated {
            private boolean didServiceRegister;
            private boolean didServiceUnregister;
            private boolean didServiceActivate;
            private boolean didServiceInactivate;

            @Override
            public void onServiceActive() {
                this.didServiceActivate = true;
                activated.add(this);
            }

            @Override
            public void onServiceInactive() {
                this.didServiceInactivate = true;
                inactivated.add(this);
            }

            @Override
            public void onServiceRegistered() {
                this.didServiceRegister = true;
                serviceRegistered.add(this);
            }

            @Override
            public void onServiceUnregistered() {
                this.didServiceUnregister = true;
                serviceUnregistered.add(this);
            }
        }

        class ChildKey
                extends TestKey
                implements ScopeKey.Child {
            private final List<String> parentScopes;

            ChildKey(String name, List<String> parentScopes) {
                super(name);
                this.parentScopes = parentScopes;
            }

            @NonNull
            @Override
            public List<String> getParentScopes() {
                return parentScopes;
            }
        }

        StateChanger stateChanger = new StateChanger() {
            @Override
            public void handleStateChange(@NonNull StateChange stateChange, @NonNull Callback completionCallback) {
                completionCallback.stateChangeComplete();
            }
        };

        final Service service1 = new Service();
        final Service service2 = new Service();

        Backstack backstack = new Backstack();
        backstack.setScopedServices(new ScopedServices() {
            @Override
            public void bindServices(@NonNull ServiceBinder serviceBinder) {
                if(serviceBinder.getScopeTag().equals("boop")) {
                    serviceBinder.addService("service", service1);
                }
                if(serviceBinder.getScopeTag().equals("beep")) {
                    serviceBinder.addService("service", service2);
                }
            }
        });

        backstack.setup(History.of(
                new ChildKey("hello", History.of("boop")),
                new ChildKey("world", History.of("beep"))
        ));
        backstack.setStateChanger(stateChanger);

        assertThat(backstack.hasScope("boop")).isTrue();
        assertThat(backstack.hasScope("beep")).isTrue();
        assertThat(backstack.getService("boop", "service")).isSameAs(service1);
        assertThat(backstack.getService("beep", "service")).isSameAs(service2);

        assertThat(backstack.lookupService("service")).isSameAs(service2);

        backstack.goBack();

        assertThat(backstack.hasScope("boop")).isTrue();
        assertThat(backstack.hasScope("beep")).isFalse();
        assertThat(backstack.getService("boop", "service")).isSameAs(service1);
        assertThat(backstack.lookupService("service")).isSameAs(service1);
    }

    @Test
    public void lookupFromScopeWorksWithExplicitParentsInTheCorrectOrder() {
        class ChildKey
                extends TestKey
                implements ScopeKey, ScopeKey.Child {
            private final List<String> parentScopes;

            ChildKey(String name, List<String> parentScopes) {
                super(name);
                this.parentScopes = parentScopes;
            }

            @NonNull
            @Override
            public List<String> getParentScopes() {
                return parentScopes;
            }

            @NonNull
            @Override
            public String getScopeTag() {
                return name;
            }
        }

        StateChanger stateChanger = new StateChanger() {
            @Override
            public void handleStateChange(@NonNull StateChange stateChange, @NonNull Callback completionCallback) {
                completionCallback.stateChangeComplete();
            }
        };

        final Object service1 = new Object();
        final Object service2 = new Object();
        final Object common1 = new Object();
        final Object common2 = new Object();

        Backstack backstack = new Backstack();
        backstack.setScopedServices(new ScopedServices() {
            @Override
            public void bindServices(@NonNull ServiceBinder serviceBinder) {
                if(serviceBinder.getScopeTag().equals("boop")) {
                    serviceBinder.addService("service1", service1);
                    serviceBinder.addService("common", common1);
                }
                if(serviceBinder.getScopeTag().equals("beep")) {
                    serviceBinder.addService("service2", service2);
                    serviceBinder.addService("common", common2);
                }
            }
        });

        backstack.setup(History.of(
                new ChildKey("hello", History.of("beep")),
                new ChildKey("world", History.of("beep", "boop")),
                new ChildKey("kappa", Collections.<String>emptyList())
        ));
        backstack.setStateChanger(stateChanger);

        assertThat(backstack.lookupService("service1")).isSameAs(service1);

        assertThat(backstack.canFindFromScope("hello", "service1")).isFalse();
        assertThat(backstack.canFindFromScope("world", "service1")).isTrue();
        assertThat(backstack.canFindFromScope("hello", "service2")).isTrue();
        try {
            assertThat(backstack.lookupFromScope("hello", "service1"));
            Assert.fail();
        } catch(IllegalStateException e) {
            // OK!
        }
        assertThat(backstack.lookupFromScope("hello", "service2")).isSameAs(service2);
        assertThat(backstack.lookupFromScope("world", "service1")).isSameAs(service1);
        assertThat(backstack.lookupFromScope("world", "service2")).isSameAs(service2);

        assertThat(backstack.canFindFromScope("world", "common")).isTrue();
        assertThat(backstack.canFindFromScope("hello", "common")).isTrue();
        assertThat(backstack.lookupFromScope("hello", "common")).isSameAs(common2);
        assertThat(backstack.lookupFromScope("world", "common")).isSameAs(common1);

        assertThat(backstack.canFindFromScope("boop", "service2")).isTrue();
        assertThat(backstack.lookupFromScope("boop", "service2")).isSameAs(service2);
        assertThat(backstack.canFindFromScope("boop", "service1")).isTrue();
        assertThat(backstack.lookupFromScope("boop", "service1")).isSameAs(service1);

        assertThat(backstack.canFindFromScope("beep", "service1")).isFalse();
        assertThat(backstack.canFindFromScope("beep", "service2")).isTrue();
        assertThat(backstack.lookupFromScope("beep", "service2")).isSameAs(service2);

        assertThat(backstack.lookupFromScope("boop", "common")).isSameAs(common1);
        assertThat(backstack.lookupFromScope("beep", "common")).isSameAs(common2);

        backstack.jumpToRoot();

        assertThat(backstack.canFindFromScope("boop", "common")).isFalse();
        assertThat(backstack.canFindFromScope("beep", "common")).isTrue();
        assertThat(backstack.lookupFromScope("beep", "common")).isSameAs(common2);

        backstack.finalizeScopes();

        assertThat(backstack.canFindFromScope("boop", "common")).isFalse();
        assertThat(backstack.canFindFromScope("beep", "common")).isFalse();
    }

    @Test
    public void explicitParentServicesReceiveExitAndActivationOnFinalizeInReversedOrder() {
        final List<Object> serviceRegistered = new ArrayList<>();
        final List<Object> serviceUnregistered = new ArrayList<>();

        final List<Object> activated = new ArrayList<>();
        final List<Object> inactivated = new ArrayList<>();
        class Service
                implements ScopedServices.Registered, ScopedServices.Activated {
            private boolean didServiceRegister;
            private boolean didServiceUnregister;
            private boolean didServiceActivate;
            private boolean didServiceInactivate;

            private final String name;

            Service(String name) {
                this.name = name;
            }

            @Override
            public String toString() {
                return "Service[" + name + "]";
            }

            @Override
            public void onServiceActive() {
                this.didServiceActivate = true;
                activated.add(this);
            }

            @Override
            public void onServiceInactive() {
                this.didServiceInactivate = true;
                inactivated.add(this);
            }

            @Override
            public void onServiceRegistered() {
                this.didServiceRegister = true;
                serviceRegistered.add(this);
            }

            @Override
            public void onServiceUnregistered() {
                this.didServiceUnregister = true;
                serviceUnregistered.add(this);
            }
        }
        final Service serviceP0 = new Service("serviceP0");
        final Service serviceP1 = new Service("serviceP1");
        final Service serviceP2 = new Service("serviceP2");
        final Service serviceC1 = new Service("serviceC1");

        class ChildKey
                extends TestKey
                implements ScopeKey, ScopeKey.Child {
            private final List<String> parentScopes;

            ChildKey(String name, List<String> parentScopes) {
                super(name);
                this.parentScopes = parentScopes;
            }

            @NonNull
            @Override
            public String getScopeTag() {
                return name;
            }

            @NonNull
            @Override
            public List<String> getParentScopes() {
                return parentScopes;
            }
        }

        StateChanger stateChanger = new StateChanger() {
            @Override
            public void handleStateChange(@NonNull StateChange stateChange, @NonNull Callback completionCallback) {
                completionCallback.stateChangeComplete();
            }
        };

        Backstack backstack = new Backstack();
        backstack.setScopedServices(new ScopedServices() {
            @Override
            public void bindServices(@NonNull ServiceBinder serviceBinder) {
                if(serviceBinder.getScopeTag().equals("P0")) {
                    serviceBinder.addService("service", serviceP0);
                }
                if(serviceBinder.getScopeTag().equals("P1")) {
                    serviceBinder.addService("service", serviceP1);
                }
                if(serviceBinder.getScopeTag().equals("P2")) {
                    serviceBinder.addService("service", serviceP2);
                }
                if(serviceBinder.getScopeTag().equals("C1")) {
                    serviceBinder.addService("service", serviceC1);
                }
            }
        });

        /*
         *                    PARENT0
         *            PARENT1
         *     PARENT2
         *   CHILD1
         */
        backstack.setup(
                History.of(
                        new ChildKey("C1", History.of("P0", "P1", "P2"))
                )
        );
        backstack.setStateChanger(stateChanger);

        assertThat(backstack.hasScope("C1")).isTrue();
        assertThat(backstack.hasScope("P0")).isTrue();
        assertThat(backstack.hasScope("P1")).isTrue();
        assertThat(backstack.hasScope("P2")).isTrue();

        assertThat(backstack.getService("C1", "service")).isSameAs(serviceC1);
        assertThat(backstack.getService("P0", "service")).isSameAs(serviceP0);
        assertThat(backstack.getService("P1", "service")).isSameAs(serviceP1);
        assertThat(backstack.getService("P2", "service")).isSameAs(serviceP2);

        /// verified set up

        assertThat(serviceRegistered).containsExactly(serviceP0, serviceP1, serviceP2, serviceC1);
        assertThat(activated).containsExactly(serviceP0, serviceP1, serviceP2, serviceC1);
        assertThat(inactivated).isEmpty();
        assertThat(serviceUnregistered).isEmpty();

        backstack.finalizeScopes();

        assertThat(inactivated).containsExactly(serviceC1, serviceP2, serviceP1, serviceP0);
        assertThat(serviceUnregistered).containsExactly(serviceC1, serviceP2, serviceP1, serviceP0);
    }
}
