/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

var fdView = angular.module('fdView', [
  'ngCookies',
  'ngResource',
  'ngSanitize',
  'ui.router',
  'ngAnimate',
  'ui.bootstrap',
  'toastr',
  'ngTagsInput',
  'ngProgress',
  'angularMoment',
  'config',
  'fdView.directives',
  'http-auth-interceptor',
  'ab.graph.matrix.directives',
  'ng.jsoneditor'
])
  .config(['$stateProvider','$urlRouterProvider','USER_ROLES', function ($stateProvider, $urlRouterProvider, USER_ROLES) {
    $stateProvider
      .state('welcome', {
        url: '/',
        templateUrl: 'views/welcome.html',
        data: {
          authorizedRoles: [USER_ROLES.user]
        }
      })
      .state('login', {
        url: '/login',
        templateUrl: 'views/login.html',
        controller: 'LoginCtrl',
        data: {
          authorizedRoles: [USER_ROLES.all]
        }
      })
      .state('search', {
        url: '/search',
        templateUrl: 'views/search.html',
        controller: 'MetaHeaderCtrl',
        data: {
          authorizedRoles: [USER_ROLES.user]
        }
      })
      .state('analyze', {
        url: '/analyze',
        templateUrl: 'views/analyze.html',
        controller: 'AnalyzeCtrl',
        data: {
          authorizedRoles: [USER_ROLES.user]
        }
      })
      .state('explore', {
        url: '/explore',
        templateUrl: 'views/explore.html',
        controller: 'ExploreCtrl',
        data: {
          authorizedRoles: [USER_ROLES.user]
        }
      })
      .state('view', {
        url: '/view/:entityKey',
        templateUrl: 'views/viewentity.html',
        controller: 'ViewEntityCtrl',
        data: {
          authorizedRoles: [USER_ROLES.user]
        }
      })
      .state('statistics', {
        url: '/statistics',
        templateUrl: 'views/statistics.html',
        data: {
          authorizedRoles: [USER_ROLES.user]
        }
      })
      .state('import', {
        url: '/import',
        templateUrl: 'views/import.html',
        controller: 'ImportCtrl',
        data: {
          authorizedRoles: [USER_ROLES.user]
        }
      })
      .state('import.load', {
        url: '/load',
        templateUrl: 'load-profile.html',
        controller: 'LoadProfileCtrl'
      })
      .state('import.edit', {
        url: '/edit',
        templateUrl: 'show-profile.html',
        controller: 'EditProfileCtrl',
        params: {keys: null},
        resolve: {
          keys: ['$stateParams', function ($stateParams) {
            return $stateParams.keys;
          }]
        }
      })
      .state('admin', {
        url: '/admin',
        templateUrl: 'views/admin.html',
        controller: 'AdminCtrl',
        data: {
          authorizedRoles: [USER_ROLES.user]
        }
      })
      .state('settings', {
        url: '/settings',
        templateUrl: 'views/settings.html',
        controller: 'SettingsCtrl',
        data: {
          authorizedRoles: [USER_ROLES.user]
        }
      });
    $urlRouterProvider.otherwise('/welcome');
    // $locationProvider.html5Mode(false);
  }])
  .config(['toastrConfig', function(toastrConfig) {
    angular.extend(toastrConfig, {
      newestOnTop: true,
      positionClass: 'toast-bottom-center',
      preventDuplicates: true,
      target: 'body'
    });
  }])
  .run(['$rootScope', '$state', '$http', 'AuthenticationSharedService', 'Session', 'toastr', 'USER_ROLES',
    function ($rootScope, $state, $http, AuthenticationSharedService, Session, toastr, USER_ROLES) {
      // TODO NEED TO SEE
      $rootScope.msg = '';
      $rootScope.$on("$stateChangeError", console.log.bind(console));
      $rootScope.$on('$stateChangeStart', function (event, next) {
        $rootScope.isAuthorized = AuthenticationSharedService.isAuthorized;
        $rootScope.userRoles = USER_ROLES;
        AuthenticationSharedService.valid(next.data.authorizedRoles);
      });

      // Call when the the client is confirmed
      $rootScope.$on('event:auth-loginConfirmed',
        function () {
          $rootScope.authenticated = true;
          if ($state.is('login')) {
            $state.go('welcome');
          }
          $rootScope.msg = '';
        }
      );

      // Call when the 401 response is returned by the server
      $rootScope.$on('event:auth-loginRequired',
        function () {
          Session.invalidate();
          delete $rootScope.authenticated;
          if (!$state.is('settings') && !$state.is('login')) {
            $state.go('login');
          }
          if ($rootScope.msg === null || $rootScope.msg === '') {
            // $rootScope.msg = 'Please login';
          }
          toastr.warning('Please login...');
        }
      );
      // Call when the 403 response is returned by the server
      $rootScope.$on('event:auth-notAuthorized',
        function () {
          $rootScope.errorMessage = 'errors.403';
          // $rootScope.msg = 'Your user account has no access to business information';
          toastr.error('Your user account has no access to business information', 'Error');
        }
      );

      // Call when the 404 response is returned by the server
      $rootScope.$on('event:not-found',
        function () {
          $rootScope.errorMessage = 'errors.404';
          // if ($state.is('login')) {
            toastr.warning('Please, check your <a ui-sref="settings">Settings</a>','Resource not found!',
              {allowHtml: true});
          // } else {
          //   toastr.warning('Resource not found!');
          // }

        }
      );
      // Call when the 500 response is returned by the server
      $rootScope.$on('event:server-error',
        function () {
          $rootScope.errorMessage = 'errors.500';
          toastr.error('Server error!');
        }
      );
      // Call when the user logs out
      $rootScope.$on('event:auth-loginCancelled',
        function () {
          // $rootScope.msg = 'Logged out';
          $state.go('login');
          toastr.success('Successfully logged out...');
        }
      );

      // Call when the user logs out
      $rootScope.$on('event:auth-session-timeout',
        function () {
          $rootScope.msg = 'Session expired';
          $state.go('login');
        }
      );
    }]);


fdView.provider('configuration', ['engineUrl','exploreUrl', function (engineUrl, exploreUrl) {
  var config = {
    'engineUrl': localStorage.getItem('engineUrl') || getDefaultEngineUrl() || engineUrl,
    'exploreUrl': localStorage.getItem('exploreUrl') || getDefaultExploreUrl() || exploreUrl,
    'devMode': localStorage.getItem('devMode')
  };

  function getDefaultEngineUrl() {
    var _engineUrl;
    if (window.location.href.indexOf('fd-view') > -1) {
      _engineUrl = window.location.href.substring(0, window.location.href.indexOf('fd-view')) + 'fd-engine';

    } else {
      // _engineUrl = window.location.protocol + '//' + window.location.host;// + '/api';
      var port = window.location.host.indexOf(':');
      if (port > 0) {
        _engineUrl = window.location.protocol + '//' + window.location.host.substr(0, port) + ':8080';
      } else {
        _engineUrl = window.location.protocol + '//' + window.location.host;
      }

    }
    console.log('Calculated URL is ' + _engineUrl);

    return _engineUrl;
  }

  function getDefaultExploreUrl() {
    var _exploreUrl;
    if (window.location.href.indexOf('fd-view') > -1) {
      _exploreUrl = window.location.href.substring(0, window.location.href.indexOf('fd-view'));

    } else {
      var port = window.location.host.indexOf(':');
      if (port > 0) {
        _exploreUrl = window.location.protocol + '//' + window.location.host.substr(0, port) + ':8080';
      } else {
        _exploreUrl = window.location.protocol + '//' + window.location.host;
      }

    }
    console.log('Calculated URL is ' + _exploreUrl);

    return _exploreUrl;
  }

  return {
    $get: function () {
      return {
        devMode: function () {
          return config.devMode;
        },
        engineUrl: function () {
          return config.engineUrl;
        },
        exploreUrl: function () {
          return config.exploreUrl;
        },
        setEngineUrl: function (engineUrl) {
          config.engineUrl = engineUrl || config.engineUrl;
          localStorage.setItem('engineUrl', engineUrl);
        },
        setExploreUrl: function (exploreUrl) {
          config.exploreUrl = exploreUrl || config.exploreUrl;
          localStorage.setItem('exploreUrl', exploreUrl);
        },
        setDevMode: function (devMode) {
          //config.devMode = devMode || config.devMode;
          if (devMode) {
            config.devMode = devMode;
            localStorage.setItem('devMode', devMode);
          } else {
            delete config.devMode;
            localStorage.removeItem('devMode');
          }

        }

      };

    }
  };
}]);

fdView.config(['$httpProvider', function ($httpProvider) {
  $httpProvider.defaults.withCredentials = true;
}]);
