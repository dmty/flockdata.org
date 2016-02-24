'use strict';

var fdView = angular.module('fdView', [
  'ngCookies',
  'ngResource',
  'ngSanitize',
  'ngRoute',
  'ui.bootstrap',
  'ngTagsInput',
  'ngProgress',
  'angularMoment',
  'config',
  'fdView.directives',
  'http-auth-interceptor',
  'ab.graph.matrix.directives'
])
  .config(
  function ($routeProvider, $locationProvider, USER_ROLES) {
    $routeProvider
      .when('/', {
        templateUrl: 'views/welcome.html',
        access: {
          authorizedRoles: [USER_ROLES.all]
        }
      })
      .when('/login', {
        templateUrl: 'views/login.html',
        access: {
          authorizedRoles: [USER_ROLES.FD]
        }
      })
      .when('/search', {
        templateUrl: 'views/search.html',
        access: {
          authorizedRoles: [USER_ROLES.all]
        }
      })
      .when('/analyze', {
        templateUrl: 'views/analyze.html',
        controller: 'AnalyseController',
        access: {
          authorizedRoles: [USER_ROLES.all]
        }
      })
      .when('/explore', {
        templateUrl: 'views/explore.html',
        access: {
          authorizedRoles: [USER_ROLES.all]
        }
      })
      .when('/view/:metaKey', {
        templateUrl: 'views/viewentity.html',
        access: {
          authorizedRoles: [USER_ROLES.all]
        }
      })
      .when('/statistics', {
        templateUrl: 'views/statistics.html',
        access: {
          authorizedRoles: [USER_ROLES.all]
        }
      })
      .when('/settings', {
        templateUrl: 'views/settings.html',
        access: {
          authorizedRoles: [USER_ROLES.all]
        }
      })
      .otherwise({
        templateUrl: 'views/welcome.html',
        access: {
          authorizedRoles: [USER_ROLES.all]
        }
      });
    $locationProvider.html5Mode(false);
  })
  .run(['$rootScope', '$location', '$http', 'AuthenticationSharedService', 'Session', 'USER_ROLES',
    function ($rootScope, $location, $http, AuthenticationSharedService, Session, USER_ROLES) {
      // TODO NEED TO SEE
      $rootScope.msg = '';

      $rootScope.$on('$routeChangeStart', function (event, next) {
        $rootScope.isAuthorized = AuthenticationSharedService.isAuthorized;
        $rootScope.userRoles = USER_ROLES;
        AuthenticationSharedService.valid(next.access.authorizedRoles);
      });

      // Call when the the client is confirmed
      $rootScope.$on('event:auth-loginConfirmed',
        function () {
          $rootScope.authenticated = true;
          if ($location.path() === '/login') {
            $location.path('/').replace();
          }
          $rootScope.msg = '';
        }
      );

      // Call when the 401 response is returned by the server
      $rootScope.$on('event:auth-loginRequired',
        function () {
          Session.invalidate();
          delete $rootScope.authenticated;
          if ($location.path() !== '/settings' && $location.path() !== '/login') {
            $location.path('/login').replace();
          }
          if ($rootScope.msg === null || $rootScope.msg === '') {
            $rootScope.msg = 'Please login';
          }
        }
      );
      // Call when the 403 response is returned by the server
      $rootScope.$on('event:auth-forbidden',
        function () {
          $rootScope.errorMessage = 'errors.403';
          $rootScope.msg = 'Your user account has no access to business information';
        }
      );

      // Call when the user logs out
      $rootScope.$on('event:auth-loginCancelled',
        function () {
          $rootScope.msg = 'Logged out';
          $location.path('/login');
        }
      );

      // Call when the user logs out
      $rootScope.$on('event:auth-session-timeout',
        function () {
          $rootScope.msg = 'Session expired';
          $location.path('/login');
        }
      );
    }]);


fdView.provider('configuration', function (engineUrl, exploreUrl) {
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
      _engineUrl = window.location.protocol + '//' + window.location.host + '/api';
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
});

fdView.config(function ($httpProvider) {
  $httpProvider.defaults.withCredentials = true;
});
